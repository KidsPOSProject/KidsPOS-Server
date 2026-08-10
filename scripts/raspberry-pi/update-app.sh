#!/usr/bin/env bash
set -euo pipefail

REPO="${KIDSPOS_REPO:-KidsPOSProject/KidsPOS-Server}"
APP_DIR="${KIDSPOS_APP_DIR:-/home/pi/kidspos}"
JAR_NAME="${KIDSPOS_JAR_NAME:-kidspos.jar}"
SERVICE="${KIDSPOS_SERVICE:-kidspos}"
HEALTH_URL="${KIDSPOS_HEALTH_URL:-http://localhost:8080/api/status}"
HEALTH_RETRIES="${KIDSPOS_HEALTH_RETRIES:-60}"
BACKUP_KEEP="${KIDSPOS_BACKUP_KEEP:-5}"
ASSET_NAME="app.jar"

JAR_PATH="${APP_DIR}/${JAR_NAME}"
DB_PATH="${APP_DIR}/kidspos.db"
BACKUP_DIR="${APP_DIR}/backup"
VERSION_FILE="${APP_DIR}/.installed-version"

usage() {
    echo "Usage:"
    echo "  sudo $0                  GitHub Releases から最新の ${ASSET_NAME} を取得して更新（要インターネット接続）"
    echo "  sudo $0 <path/to/jar>    手元に持ち込んだ jar ファイルで更新（オフライン運用）"
    echo "  sudo $0 --force          同一バージョンでも強制的に再インストール"
    exit 1
}

log() { echo "[update-app] $*"; }
fail() { echo "[update-app] ERROR: $*" >&2; exit 1; }

FORCE=false
LOCAL_JAR=""
for arg in "$@"; do
    case "$arg" in
        --force) FORCE=true ;;
        -h|--help) usage ;;
        *) LOCAL_JAR="$arg" ;;
    esac
done

[ -d "$APP_DIR" ] || fail "アプリケーションディレクトリがありません: $APP_DIR"
command -v curl >/dev/null || fail "curl が必要です"

TMP_DIR=$(mktemp -d)
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
    RELEASE_INFO=$(curl -fsSL "https://api.github.com/repos/${REPO}/releases?per_page=20" | python3 -c '
import json, sys
for release in json.load(sys.stdin):
    if release.get("draft") or release.get("prerelease"):
        continue
    for asset in release.get("assets", []):
        if asset["name"] == "'"$ASSET_NAME"'":
            print(release["tag_name"])
            print(asset["browser_download_url"])
            sys.exit(0)
sys.exit(1)
') || fail "${ASSET_NAME} を含むリリースが見つかりません"
    NEW_VERSION=$(echo "$RELEASE_INFO" | sed -n 1p)
    DOWNLOAD_URL=$(echo "$RELEASE_INFO" | sed -n 2p)

    CURRENT_VERSION=$(cat "$VERSION_FILE" 2>/dev/null || echo "none")
    if [ "$NEW_VERSION" = "$CURRENT_VERSION" ] && [ "$FORCE" = false ]; then
        log "すでに最新です（$CURRENT_VERSION）。強制更新は --force を付けてください。"
        exit 0
    fi

    log "ダウンロード中: $NEW_VERSION"
    curl -fL --retry 3 -o "$NEW_JAR" "$DOWNLOAD_URL"
fi

head -c 2 "$NEW_JAR" | grep -q "PK" || fail "取得したファイルが jar 形式ではありません"

STAMP="$(date +%Y%m%d%H%M%S)"
mkdir -p "$BACKUP_DIR"

log "サービスを停止します: $SERVICE"
sudo systemctl stop "$SERVICE"

# DB バックアップは必ずサービス停止後に行う（起動中のコピーは破損の恐れ）
DB_BACKUP=""
if [ -f "$DB_PATH" ]; then
    DB_BACKUP="${BACKUP_DIR}/kidspos.db.${STAMP}"
    cp "$DB_PATH" "$DB_BACKUP"
    log "DB をバックアップしました: $DB_BACKUP"
fi
JAR_BACKUP=""
if [ -f "$JAR_PATH" ]; then
    JAR_BACKUP="${BACKUP_DIR}/${JAR_NAME}.${STAMP}"
    cp "$JAR_PATH" "$JAR_BACKUP"
fi

rollback() {
    log "巻き戻しを実行します..."
    sudo systemctl stop "$SERVICE" || true
    [ -n "$JAR_BACKUP" ] && cp "$JAR_BACKUP" "$JAR_PATH"
    [ -n "$DB_BACKUP" ] && cp "$DB_BACKUP" "$DB_PATH"
    sudo systemctl start "$SERVICE" || true
    fail "更新に失敗したため旧バージョンに戻しました。ログを確認してください: journalctl -u $SERVICE -n 100"
}

cp "$NEW_JAR" "$JAR_PATH"

log "サービスを起動します（Flyway マイグレーションが自動適用されます）"
sudo systemctl start "$SERVICE"

log "ヘルスチェック中: $HEALTH_URL"
HEALTHY=false
for _ in $(seq 1 "$HEALTH_RETRIES"); do
    if curl -fsS -o /dev/null "$HEALTH_URL"; then
        HEALTHY=true
        break
    fi
    sleep 2
done
[ "$HEALTHY" = true ] || rollback

echo "$NEW_VERSION" > "$VERSION_FILE"

# 古いバックアップを世代数で削減
for prefix in "kidspos.db." "${JAR_NAME}."; do
    ls -1t "${BACKUP_DIR}/${prefix}"* 2>/dev/null | tail -n +$((BACKUP_KEEP + 1)) | xargs -r rm -f
done

log "更新が完了しました: $NEW_VERSION"
