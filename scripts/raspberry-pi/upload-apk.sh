#!/usr/bin/env bash
set -Eeuo pipefail

REPO="${KIDSPOS_ANDROID_REPO:-KidsPOSProject/KidsPOS-for-Android}"
SERVER_URL="${KIDSPOS_SERVER_URL:-http://localhost:8080}"
UPLOAD_TIMEOUT="${KIDSPOS_UPLOAD_TIMEOUT:-600}"

usage() {
    echo "Usage:"
    echo "  $0                      GitHub Releases から最新の APK を取得して登録（要インターネット接続）"
    echo "  $0 <path/to/apk>        手元に持ち込んだ APK を登録（オフライン運用）"
    echo ""
    echo "Options:"
    echo "  --server <URL>          登録先サーバー（既定: ${SERVER_URL}）"
    echo "  --notes <TEXT>          リリースノート（省略時はリリースのタグと本文を使用）"
    exit 1
}

log() { echo "[upload-apk] $*"; }
fail() { echo "[upload-apk] ERROR: $*" >&2; exit 1; }

LOCAL_APK=""
NOTES=""
while [ $# -gt 0 ]; do
    case "$1" in
        --server)
            [ $# -ge 2 ] || fail "--server には URL を指定してください"
            SERVER_URL="$2"
            shift 2
            ;;
        --server=*)
            SERVER_URL="${1#*=}"
            shift
            ;;
        --notes)
            [ $# -ge 2 ] || fail "--notes にはテキストを指定してください"
            NOTES="$2"
            shift 2
            ;;
        --notes=*)
            NOTES="${1#*=}"
            shift
            ;;
        -h|--help) usage ;;
        -*)
            echo "[upload-apk] 不明なオプション: $1" >&2
            usage
            ;;
        *)
            LOCAL_APK="$1"
            shift
            ;;
    esac
done

SERVER_URL="${SERVER_URL%/}"
[ -n "$SERVER_URL" ] || fail "登録先サーバーが空です"
command -v curl >/dev/null || fail "curl が必要です"

# /tmp は tmpfs で容量が小さいため、ディスク上の /var/tmp を使う
TMP_DIR=$(mktemp -d /var/tmp/kidspos-upload.XXXXXXXX)
trap 'rm -rf "$TMP_DIR"' EXIT

if [ -n "$LOCAL_APK" ]; then
    [ -f "$LOCAL_APK" ] || fail "ファイルが見つかりません: $LOCAL_APK"
    APK_PATH="$LOCAL_APK"
    log "ローカルファイルを登録します: $LOCAL_APK"
else
    command -v python3 >/dev/null || fail "python3 が必要です（Raspberry Pi OS には標準搭載）"
    log "GitHub Releases から最新の APK を探しています: $REPO"
    RELEASE_INFO=$(curl -fsSL "https://api.github.com/repos/${REPO}/releases?per_page=20" | python3 -c '
import json, sys
for release in json.load(sys.stdin):
    if release.get("draft") or release.get("prerelease"):
        continue
    for asset in release.get("assets", []):
        if asset["name"].endswith(".apk"):
            print(release["tag_name"])
            print(asset["name"])
            print(asset["browser_download_url"])
            print(" ".join((release.get("body") or "").split())[:500])
            sys.exit(0)
sys.exit(1)
') || fail "APK を含むリリースが見つかりません"
    RELEASE_TAG=$(echo "$RELEASE_INFO" | sed -n 1p)
    ASSET_NAME=$(echo "$RELEASE_INFO" | sed -n 2p)
    DOWNLOAD_URL=$(echo "$RELEASE_INFO" | sed -n 3p)
    RELEASE_BODY=$(echo "$RELEASE_INFO" | sed -n 4p)

    APK_PATH="${TMP_DIR}/${ASSET_NAME}"
    log "ダウンロード中: ${RELEASE_TAG} (${ASSET_NAME})"
    curl -fL --retry 3 -o "$APK_PATH" "$DOWNLOAD_URL" || fail "APK のダウンロードに失敗しました"

    if [ -z "$NOTES" ]; then
        if [ -n "$RELEASE_BODY" ]; then
            NOTES="${RELEASE_TAG} ${RELEASE_BODY}"
        else
            NOTES="$RELEASE_TAG"
        fi
    fi
fi

head -c 2 "$APK_PATH" | grep -q "PK" || fail "取得したファイルが APK 形式ではありません: $APK_PATH"

BODY_FILE="${TMP_DIR}/response.json"
CURL_ARGS=(-sS -X POST --max-time "$UPLOAD_TIMEOUT" -o "$BODY_FILE" -w '%{http_code}' -F "file=@${APK_PATH}")
if [ -n "$NOTES" ]; then
    CURL_ARGS+=(-F "releaseNotes=${NOTES}")
fi

log "サーバーへ登録します: ${SERVER_URL}/api/apk/upload"
HTTP_CODE=$(curl "${CURL_ARGS[@]}" "${SERVER_URL}/api/apk/upload") ||
    fail "サーバーに接続できませんでした: ${SERVER_URL}"

json_field() {
    command -v python3 >/dev/null || return 1
    python3 -c '
import json, sys
try:
    data = json.load(sys.stdin)
except Exception:
    sys.exit(1)
value = data.get(sys.argv[1])
if value is None:
    sys.exit(1)
print(value)
' "$1" < "$BODY_FILE" 2>/dev/null
}

case "$HTTP_CODE" in
    201)
        VERSION=$(json_field version || echo "")
        VERSION_CODE=$(json_field versionCode || echo "")
        if [ -n "$VERSION" ]; then
            log "登録しました: ${VERSION} (versionCode=${VERSION_CODE})"
        else
            log "登録しました"
        fi
        log "タブレットの設定画面からアップデートを実行してください"
        ;;
    409)
        MESSAGE=$(json_field message || echo "同じバージョンが既に登録されています")
        log "登録済みのため何もしませんでした: ${MESSAGE}"
        ;;
    *)
        MESSAGE=$(json_field message || echo "")
        if [ -n "$MESSAGE" ]; then
            echo "[upload-apk] サーバーの応答: ${MESSAGE}" >&2
        fi
        fail "登録に失敗しました（HTTP ${HTTP_CODE}）"
        ;;
esac
