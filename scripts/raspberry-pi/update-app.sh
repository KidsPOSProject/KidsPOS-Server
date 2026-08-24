#!/usr/bin/env bash
set -Eeuo pipefail

REPO="${KIDSPOS_REPO:-KidsPOSProject/KidsPOS-Server}"
APP_DIR="${KIDSPOS_APP_DIR:-/opt/kidspos}"
JAR_NAME="${KIDSPOS_JAR_NAME:-app.jar}"
SERVICE="${KIDSPOS_SERVICE:-kidspos-server}"
HEALTH_URL="${KIDSPOS_HEALTH_URL:-http://localhost:8080/api/status}"
HEALTH_RETRIES="${KIDSPOS_HEALTH_RETRIES:-600}"
HEALTH_TIMEOUT="${KIDSPOS_HEALTH_TIMEOUT:-10}"
BACKUP_KEEP="${KIDSPOS_BACKUP_KEEP:-5}"
SERVER_URL="${KIDSPOS_SERVER_URL:-http://localhost:8080}"
ASSET_NAME="app.jar"
SCRIPTS_ASSET="kidspos-scripts.tar.gz"
MANAGED_SCRIPTS="update-app.sh doctor.sh upload-apk.sh"
DISPLAY_DIR="${KIDSPOS_DISPLAY_DIR:-/opt/kidspos-display}"
DISPLAY_SERVICE="${KIDSPOS_DISPLAY_SERVICE:-kidspos-display}"
DISPLAY_FILES="app.py config.py health.py layout.py renderer.py epaper.py"

JAR_PATH="${APP_DIR}/${JAR_NAME}"
DB_PATH="${APP_DIR}/kidspos.db"
BACKUP_DIR="${APP_DIR}/backup"
VERSION_FILE="${APP_DIR}/.installed-version"
SCRIPTS_VERSION_FILE="${APP_DIR}/.installed-scripts-version"
STAGE_DIR="${APP_DIR}/.scripts-update"
UPLOAD_SCRIPT="${APP_DIR}/upload-apk.sh"

usage() {
    echo "Usage:"
    echo "  sudo $0                    GitHub Releases から最新の ${ASSET_NAME} を取得して更新（要インターネット接続）"
    echo "  sudo $0 <path/to/jar>      手元に持ち込んだ jar ファイルで更新（オフライン運用）"
    echo "  sudo $0 --force            同一バージョンでも強制的に再インストール"
    echo "  sudo $0 --skip-apk         サーバーの更新のみ行い、APK の確認は行わない"
    echo "  sudo $0 --skip-self-update スクリプト自身の更新は行わない"
    echo "  sudo $0 --skip-display     e-Paper 表示サービスの更新は行わない"
    exit 1
}

log() { echo "[update-app] $*"; }
fail() { echo "[update-app] ERROR: $*" >&2; exit 1; }

FORCE=false
SKIP_APK=false
SKIP_SELF_UPDATE=false
SKIP_DISPLAY=false
LOCAL_JAR=""
SCRIPTS_URL=""
for arg in "$@"; do
    case "$arg" in
        --force) FORCE=true ;;
        --skip-apk) SKIP_APK=true ;;
        --skip-self-update) SKIP_SELF_UPDATE=true ;;
        --skip-display) SKIP_DISPLAY=true ;;
        -h|--help) usage ;;
        -*)
            echo "[update-app] 不明なオプション: $arg" >&2
            usage
            ;;
        *) LOCAL_JAR="$arg" ;;
    esac
done

# 表示サービスを導入していない Pi もあるため、配置先が無ければ何もしない。
# 差し替えに失敗した場合は呼び出し元でバージョンを記録させず、次回の更新で再試行させる
update_display() {
    if [ "$SKIP_DISPLAY" = true ]; then
        return 0
    fi
    if [ ! -d "$DISPLAY_DIR" ]; then
        log "表示サービスが導入されていないため更新しません: $DISPLAY_DIR"
        return 0
    fi

    local staged="${STAGE_DIR}/raspberry-pi-display"
    if [ ! -d "$staged" ]; then
        log "WARN: 配布物に raspberry-pi-display が含まれていません"
        return 1
    fi

    local ok=true
    local module src
    for module in $DISPLAY_FILES; do
        src="${staged}/${module}"
        if [ ! -f "$src" ]; then
            log "WARN: 配布物に含まれていません: raspberry-pi-display/${module}"
            ok=false
            continue
        fi
        if ! mv -f "$src" "${DISPLAY_DIR}/${module}"; then
            log "WARN: 差し替えに失敗しました: ${DISPLAY_DIR}/${module}"
            ok=false
            continue
        fi
        # install-display.sh がサービス実行ユーザーに所有者を合わせているため、
        # root で置き直したファイルも配置先ディレクトリと同じ所有者に戻す
        if ! chown --reference="$DISPLAY_DIR" "${DISPLAY_DIR}/${module}"; then
            log "WARN: 所有者を合わせられませんでした: ${DISPLAY_DIR}/${module}"
        fi
    done
    if [ -f "${DISPLAY_DIR}/app.py" ]; then
        chmod +x "${DISPLAY_DIR}/app.py"
    fi

    if [ "$ok" != true ]; then
        return 1
    fi

    log "表示サービスを更新しました: $DISPLAY_DIR"
    if systemctl is-active --quiet "$DISPLAY_SERVICE"; then
        if ! sudo systemctl restart "$DISPLAY_SERVICE"; then
            log "WARN: 表示サービスの再起動に失敗しました: $DISPLAY_SERVICE"
            return 1
        fi
    else
        log "表示サービスは停止中のため再起動しません: $DISPLAY_SERVICE"
    fi
    return 0
}

# スクリプトの更新に失敗してもサーバーの更新は完了しているため、警告のみで成功扱いにする。
# 成功した分だけ差し替え、全て成功したときだけバージョンを記録して次回の再試行に備える
self_update() {
    if [ "$SKIP_SELF_UPDATE" = true ]; then
        return 0
    fi
    if [ -n "$LOCAL_JAR" ]; then
        return 0
    fi
    if [ -z "$SCRIPTS_URL" ]; then
        log "リリースに ${SCRIPTS_ASSET} が無いためスクリプトの更新は行いません"
        return 0
    fi
    local current
    current=$(cat "$SCRIPTS_VERSION_FILE" 2>/dev/null || echo "none")
    if [ "$current" = "$NEW_VERSION" ] && [ "$FORCE" = false ]; then
        return 0
    fi

    log "スクリプトを更新します: $NEW_VERSION"
    rm -rf "$STAGE_DIR"
    if ! mkdir -p "$STAGE_DIR"; then
        log "WARN: スクリプト更新用ディレクトリを作成できません: $STAGE_DIR"
        return 0
    fi
    if ! curl -fL --retry 3 -o "${STAGE_DIR}/${SCRIPTS_ASSET}" "$SCRIPTS_URL"; then
        log "WARN: スクリプトの取得に失敗しました。サーバーの更新は完了しています"
        rm -rf "$STAGE_DIR"
        return 0
    fi
    if ! tar -xzf "${STAGE_DIR}/${SCRIPTS_ASSET}" -C "$STAGE_DIR"; then
        log "WARN: スクリプトの展開に失敗しました。サーバーの更新は完了しています"
        rm -rf "$STAGE_DIR"
        return 0
    fi

    local all_ok=true
    local script src
    for script in $MANAGED_SCRIPTS; do
        src="${STAGE_DIR}/raspberry-pi/${script}"
        if [ ! -f "$src" ]; then
            log "WARN: 配布物に含まれていません: $script"
            all_ok=false
            continue
        fi
        chmod +x "$src"
        # 実行中の自分自身を上書きすると bash の読み込みが壊れるため、
        # 同一ファイルシステム上に展開してから mv で差し替える
        if mv -f "$src" "${APP_DIR}/${script}"; then
            log "更新しました: ${APP_DIR}/${script}"
        else
            log "WARN: 差し替えに失敗しました: ${APP_DIR}/${script}"
            all_ok=false
        fi
    done
    if ! update_display; then
        all_ok=false
    fi
    rm -rf "$STAGE_DIR"

    if [ "$all_ok" = true ]; then
        echo "$NEW_VERSION" > "$SCRIPTS_VERSION_FILE"
    fi
}

# APK の登録に失敗してもサーバーの更新は完了しているため、警告のみで成功扱いにする
sync_apk() {
    if [ "$SKIP_APK" = true ]; then
        return 0
    fi
    if [ -n "$LOCAL_JAR" ]; then
        log "オフライン更新のため APK の確認は行いません（登録する場合: ${UPLOAD_SCRIPT} <path/to/apk>）"
        return 0
    fi
    if [ ! -x "$UPLOAD_SCRIPT" ]; then
        log "APK 登録スクリプトが無いため APK の確認は行いません: $UPLOAD_SCRIPT"
        return 0
    fi
    log "最新の APK を確認します"
    if ! "$UPLOAD_SCRIPT" --server "$SERVER_URL"; then
        log "WARN: APK の更新に失敗しました。サーバーの更新は完了しています"
    fi
}

[ -d "$APP_DIR" ] || fail "アプリケーションディレクトリがありません: $APP_DIR"
command -v curl >/dev/null || fail "curl が必要です"

# /tmp は tmpfs で容量が小さいため、ディスク上の /var/tmp を使う
TMP_DIR=$(mktemp -d /var/tmp/kidspos-update.XXXXXXXX)
trap 'rm -rf "$TMP_DIR"' EXIT
NEW_JAR="${TMP_DIR}/new.jar"
NEW_VERSION=""

if [ -n "$LOCAL_JAR" ]; then
    [ -f "$LOCAL_JAR" ] || fail "ファイルが見つかりません: $LOCAL_JAR"
    cp "$LOCAL_JAR" "$NEW_JAR"
    NEW_VERSION="local-$(date +%Y%m%d%H%M%S)"
    log "ローカルファイルから更新します: $LOCAL_JAR"
else
    command -v python3 >/dev/null || fail "python3 が必要です（Raspberry Pi OS には標準搭載）"
    log "GitHub Releases から最新の ${ASSET_NAME} を探しています..."
    RELEASE_INFO=$(curl -fsSL "https://api.github.com/repos/${REPO}/releases?per_page=20" |
        ASSET_NAME="$ASSET_NAME" SCRIPTS_ASSET="$SCRIPTS_ASSET" python3 -c '
import json, os, sys
jar_name = os.environ["ASSET_NAME"]
scripts_name = os.environ["SCRIPTS_ASSET"]
for release in json.load(sys.stdin):
    if release.get("draft") or release.get("prerelease"):
        continue
    assets = {a["name"]: a["browser_download_url"] for a in release.get("assets", [])}
    if jar_name in assets:
        print(release["tag_name"])
        print(assets[jar_name])
        print(assets.get(scripts_name, ""))
        sys.exit(0)
sys.exit(1)
') || fail "${ASSET_NAME} を含むリリースが見つかりません"
    NEW_VERSION=$(echo "$RELEASE_INFO" | sed -n 1p)
    DOWNLOAD_URL=$(echo "$RELEASE_INFO" | sed -n 2p)
    SCRIPTS_URL=$(echo "$RELEASE_INFO" | sed -n 3p)

    CURRENT_VERSION=$(cat "$VERSION_FILE" 2>/dev/null || echo "none")
    if [ "$NEW_VERSION" = "$CURRENT_VERSION" ] && [ "$FORCE" = false ]; then
        log "すでに最新です（$CURRENT_VERSION）。強制更新は --force を付けてください。"
        self_update
        sync_apk
        exit 0
    fi

    log "ダウンロード中: $NEW_VERSION"
    curl -fL --retry 3 -o "$NEW_JAR" "$DOWNLOAD_URL"
fi

head -c 2 "$NEW_JAR" | grep -q "PK" || fail "取得したファイルが jar 形式ではありません"

STAMP="$(date +%Y%m%d%H%M%S)"
mkdir -p "$BACKUP_DIR"

DB_BACKUP=""
JAR_BACKUP=""

rollback() {
    trap - ERR
    log "巻き戻しを実行します..."
    sudo systemctl stop "$SERVICE" || true
    if [ -n "$JAR_BACKUP" ]; then
        cp "$JAR_BACKUP" "$JAR_PATH"
    fi
    if [ -n "$DB_BACKUP" ]; then
        cp "$DB_BACKUP" "$DB_PATH"
    fi
    sudo systemctl start "$SERVICE" || true
    fail "更新に失敗したため旧バージョンに戻しました。ログを確認してください: journalctl -u $SERVICE -n 100"
}

log "サービスを停止します: $SERVICE"
sudo systemctl stop "$SERVICE"

# ここから先の失敗はすべて巻き戻しに誘導する
trap 'rollback' ERR

# DB バックアップは必ずサービス停止後に行う（起動中のコピーは破損の恐れ）
if [ -f "$DB_PATH" ]; then
    DB_BACKUP="${BACKUP_DIR}/kidspos.db.${STAMP}"
    cp "$DB_PATH" "$DB_BACKUP"
    log "DB をバックアップしました: $DB_BACKUP"
fi
if [ -f "$JAR_PATH" ]; then
    JAR_BACKUP="${BACKUP_DIR}/${JAR_NAME}.${STAMP}"
    cp "$JAR_PATH" "$JAR_BACKUP"
fi

cp "$NEW_JAR" "$JAR_PATH"

log "サービスを起動します（Flyway マイグレーションが自動適用されます）"
sudo systemctl start "$SERVICE"

log "ヘルスチェック中: $HEALTH_URL"
HEALTHY=false
for _ in $(seq 1 "$HEALTH_RETRIES"); do
    if curl -fsS --max-time "$HEALTH_TIMEOUT" -o /dev/null "$HEALTH_URL"; then
        HEALTHY=true
        break
    fi
    sleep 2
done
[ "$HEALTHY" = true ] || rollback

trap - ERR

echo "$NEW_VERSION" > "$VERSION_FILE"

# 古いバックアップを世代数で削減する。ファイル名末尾のタイムスタンプで並べるため、
# コピー等で mtime が変わっていても新しい世代を取り違えない。該当が無い場合も成功扱いにする
for prefix in "kidspos.db." "${JAR_NAME}."; do
    ls -1 "${BACKUP_DIR}/${prefix}"* 2>/dev/null | sort -r | tail -n +$((BACKUP_KEEP + 1)) | xargs -r rm -f || true
done

log "更新が完了しました: $NEW_VERSION"

self_update
sync_apk
