#!/usr/bin/env bash
set -uo pipefail

REPO="${KIDSPOS_REPO:-KidsPOSProject/KidsPOS-Server}"
APP_DIR="${KIDSPOS_APP_DIR:-/opt/kidspos}"
JAR_NAME="${KIDSPOS_JAR_NAME:-app.jar}"
SERVICE="${KIDSPOS_SERVICE:-kidspos-server}"
HEALTH_URL="${KIDSPOS_HEALTH_URL:-http://localhost:8080/api/status}"
REQUIRED_JAVA_MAJOR="${KIDSPOS_REQUIRED_JAVA_MAJOR:-21}"
DISK_WARN_MB="${KIDSPOS_DISK_WARN_MB:-500}"
DISK_NG_MB="${KIDSPOS_DISK_NG_MB:-100}"
UNIT_DIR="${KIDSPOS_UNIT_DIR:-/etc/systemd/system}"
LOG_LINES="${KIDSPOS_LOG_LINES:-500}"
ASSET_NAME="app.jar"

JAR_PATH="${APP_DIR}/${JAR_NAME}"
DB_PATH="${APP_DIR}/kidspos.db"
BACKUP_DIR="${APP_DIR}/backup"
UPLOAD_DIR="${APP_DIR}/uploads"
VERSION_FILE="${APP_DIR}/.installed-version"

OK_COUNT=0
WARN_COUNT=0
NG_COUNT=0

section() {
    echo ""
    echo "== $1 =="
}

ok() {
    OK_COUNT=$((OK_COUNT + 1))
    echo "[ OK ] $1"
}

warn() {
    WARN_COUNT=$((WARN_COUNT + 1))
    echo "[注意] $1"
    [ -n "${2:-}" ] && echo "       対処: $2"
    return 0
}

ng() {
    NG_COUNT=$((NG_COUNT + 1))
    echo "[ NG ] $1"
    [ -n "${2:-}" ] && echo "       対処: $2"
    return 0
}

info() {
    echo "       $1"
}

echo "KidsPOS 稼働診断"
info "対象ディレクトリ: $APP_DIR"
info "サービス名: $SERVICE"

section "Java"
if ! command -v java >/dev/null; then
    ng "java が見つかりません" "sudo apt install -y openjdk-${REQUIRED_JAVA_MAJOR}-jre-headless"
else
    JAVA_LINE=$(java -version 2>&1 | head -n1)
    JAVA_MAJOR=$(echo "$JAVA_LINE" | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p')
    if [ -z "$JAVA_MAJOR" ]; then
        warn "java のバージョンを判定できません: $JAVA_LINE"
    elif [ "$JAVA_MAJOR" -lt "$REQUIRED_JAVA_MAJOR" ]; then
        ng "Java ${REQUIRED_JAVA_MAJOR} 以上が必要です（検出: $JAVA_MAJOR）" "sudo apt install -y openjdk-${REQUIRED_JAVA_MAJOR}-jre-headless"
    else
        ok "Java $JAVA_MAJOR"
    fi
fi

section "アプリケーション"
if [ ! -d "$APP_DIR" ]; then
    ng "アプリケーションディレクトリがありません: $APP_DIR" "sudo ./install.sh"
elif [ ! -f "$JAR_PATH" ]; then
    ng "jar がありません: $JAR_PATH" "sudo ${APP_DIR}/update-app.sh"
elif ! head -c 2 "$JAR_PATH" | grep -q "PK"; then
    ng "jar が壊れています（zip 形式ではありません）: $JAR_PATH" "sudo ${APP_DIR}/update-app.sh --force"
elif ! grep -aq "BOOT-INF/" "$JAR_PATH"; then
    ng "実行可能な jar ではありません（plain jar が置かれている可能性）" "リリースの ${ASSET_NAME} を使って sudo ${APP_DIR}/update-app.sh <path> で入れ直してください"
else
    ok "jar は実行可能な形式です（$(du -h "$JAR_PATH" | cut -f1)）"
fi

CURRENT_VERSION=$(cat "$VERSION_FILE" 2>/dev/null || echo "")
if [ -z "$CURRENT_VERSION" ]; then
    warn "導入バージョンが記録されていません" "sudo ${APP_DIR}/update-app.sh --force で記録し直せます"
else
    ok "導入バージョン: $CURRENT_VERSION"
fi

if command -v curl >/dev/null && command -v python3 >/dev/null; then
    LATEST_VERSION=$(curl -fsS --max-time 10 "https://api.github.com/repos/${REPO}/releases?per_page=20" 2>/dev/null | python3 -c '
import json, sys
try:
    releases = json.load(sys.stdin)
except Exception:
    sys.exit(1)
for release in releases:
    if release.get("draft") or release.get("prerelease"):
        continue
    for asset in release.get("assets", []):
        if asset["name"] == "'"$ASSET_NAME"'":
            print(release["tag_name"])
            sys.exit(0)
sys.exit(1)
' 2>/dev/null) || LATEST_VERSION=""
    if [ -z "$LATEST_VERSION" ]; then
        info "最新リリースは確認できませんでした（オフラインの場合は正常です）"
    elif [ "$LATEST_VERSION" = "$CURRENT_VERSION" ]; then
        ok "最新リリースと同じバージョンです"
    else
        warn "より新しいリリースがあります: $LATEST_VERSION" "sudo ${APP_DIR}/update-app.sh"
    fi
fi

section "サービス"
if ! command -v systemctl >/dev/null; then
    ng "systemctl が見つかりません"
else
    if [ "$(systemctl is-enabled "$SERVICE" 2>/dev/null)" = "enabled" ]; then
        ok "自動起動が有効です"
    else
        warn "自動起動が無効です（再起動後に立ち上がりません）" "sudo systemctl enable $SERVICE"
    fi

    if systemctl is-active --quiet "$SERVICE"; then
        ok "サービスは起動しています"
        RESTARTS=$(systemctl show -p NRestarts --value "$SERVICE" 2>/dev/null || echo 0)
        case "$RESTARTS" in
            ''|*[!0-9]*) RESTARTS=0 ;;
        esac
        if [ "$RESTARTS" -ge 3 ]; then
            warn "起動後に $RESTARTS 回再起動しています（起動失敗を繰り返している可能性）" "journalctl -u $SERVICE -n 100"
        fi
    else
        ng "サービスが停止しています" "sudo systemctl start $SERVICE してから journalctl -u $SERVICE -n 100 を確認"
    fi

    # 同じ jar を起動する別名のユニットが残っていると、1 つの SQLite を 2 プロセスが掴んで破損する
    DUPLICATE_UNITS=""
    for unit_file in "${UNIT_DIR}"/*.service; do
        [ -f "$unit_file" ] || continue
        unit_name=$(basename "$unit_file" .service)
        [ "$unit_name" != "$SERVICE" ] || continue
        grep -q "^ExecStart=.*${JAR_PATH}" "$unit_file" || continue
        DUPLICATE_UNITS="${DUPLICATE_UNITS} ${unit_name}"
    done
    if [ -n "$DUPLICATE_UNITS" ]; then
        ng "同じ jar を起動するユニットが他にもあります:${DUPLICATE_UNITS}" "sudo systemctl disable --now${DUPLICATE_UNITS} で停止してください（DB が破損します）"
    else
        ok "同じ jar を起動する別のユニットはありません"
    fi
fi

section "ヘルスチェック"
if ! command -v curl >/dev/null; then
    warn "curl が無いためヘルスチェックを行えません" "sudo apt install -y curl"
elif curl -fsS --max-time 10 -o /dev/null "$HEALTH_URL"; then
    ok "$HEALTH_URL に応答があります"
else
    ng "$HEALTH_URL に応答がありません" "起動直後は数分かかります。続く場合は journalctl -u $SERVICE -n 100"
fi

PORT=$(echo "$HEALTH_URL" | sed -n 's#.*://[^/:]*:\([0-9][0-9]*\).*#\1#p')
[ -n "$PORT" ] || PORT=8080
if command -v ss >/dev/null; then
    if ss -ltn 2>/dev/null | grep -q ":${PORT}\b"; then
        ok "ポート $PORT が待ち受け中です"
    else
        warn "ポート $PORT が待ち受けていません" "サービスの起動状態とログを確認してください"
    fi
fi

section "データベース"
if [ ! -f "$DB_PATH" ]; then
    warn "DB ファイルがありません: $DB_PATH" "初回起動時に自動生成されます。起動済みで無い場合はログを確認してください"
else
    ok "DB があります（$(du -h "$DB_PATH" | cut -f1)）"
    SERVICE_USER=$(systemctl show -p User --value "$SERVICE" 2>/dev/null || echo "")
    [ -n "$SERVICE_USER" ] || SERVICE_USER="${KIDSPOS_SERVICE_USER:-pi}"
    DIR_OWNER=$(stat -c %U "$APP_DIR" 2>/dev/null || echo "")
    if [ -n "$DIR_OWNER" ] && [ "$DIR_OWNER" != "$SERVICE_USER" ]; then
        warn "$APP_DIR の所有者が $DIR_OWNER で、サービス実行ユーザー $SERVICE_USER と異なります" "sudo chown -R ${SERVICE_USER}: $APP_DIR"
    fi
fi

if [ -d "$UPLOAD_DIR" ]; then
    ok "アップロード領域があります: $UPLOAD_DIR"
else
    warn "アップロード領域がありません: $UPLOAD_DIR" "配信済みの APK が失われている可能性があります。再アップロードで復旧できます"
fi

section "ディスクとバックアップ"
AVAIL_MB=$(df -Pm "$APP_DIR" 2>/dev/null | awk 'NR==2 {print $4}')
if [ -z "$AVAIL_MB" ]; then
    warn "空き容量を取得できませんでした"
elif [ "$AVAIL_MB" -lt "$DISK_NG_MB" ]; then
    ng "空き容量が ${AVAIL_MB}MB しかありません" "${BACKUP_DIR} の古いバックアップを削除してください"
elif [ "$AVAIL_MB" -lt "$DISK_WARN_MB" ]; then
    warn "空き容量が ${AVAIL_MB}MB です" "${BACKUP_DIR} の古いバックアップを削除してください"
else
    ok "空き容量: ${AVAIL_MB}MB"
fi

DB_BACKUPS=$(ls -1 "${BACKUP_DIR}/kidspos.db."* 2>/dev/null | wc -l | tr -d ' ')
if [ "$DB_BACKUPS" -eq 0 ]; then
    warn "DB のバックアップがありません" "更新時に自動作成されます。手動なら sudo systemctl stop $SERVICE してから cp $DB_PATH ${BACKUP_DIR}/"
else
    ok "DB バックアップ: ${DB_BACKUPS} 世代"
fi

section "ログ"
if ! command -v journalctl >/dev/null; then
    info "journalctl が無いためログを確認できません"
else
    JOURNAL=$(journalctl -u "$SERVICE" -n "$LOG_LINES" --no-pager 2>/dev/null)
    if [ -z "$JOURNAL" ]; then
        info "ログがまだありません: journalctl -u $SERVICE"
    else
        RECENT_ERRORS=$(echo "$JOURNAL" | grep -cE "ERROR|Exception")
        if [ "$RECENT_ERRORS" -eq 0 ]; then
            ok "直近のログに異常はありません"
        else
            warn "直近 ${LOG_LINES} 行に ${RECENT_ERRORS} 件のエラーがあります" "journalctl -u $SERVICE -n 100"
        fi
    fi
fi

section "時刻"
if command -v timedatectl >/dev/null; then
    if [ "$(timedatectl show -p NTPSynchronized --value 2>/dev/null)" = "yes" ]; then
        ok "時刻が同期されています"
    else
        warn "時刻が同期されていません（売上の記録時刻がずれます）" "sudo timedatectl set-ntp true、オフライン運用なら sudo date -s 'YYYY-MM-DD HH:MM:SS'"
    fi
fi

echo ""
echo "結果: OK ${OK_COUNT} / 注意 ${WARN_COUNT} / NG ${NG_COUNT}"
[ "$NG_COUNT" -eq 0 ]
