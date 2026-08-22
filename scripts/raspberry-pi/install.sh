#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="${KIDSPOS_APP_DIR:-/home/pi/kidspos}"
JAR_NAME="${KIDSPOS_JAR_NAME:-kidspos.jar}"
SERVICE="${KIDSPOS_SERVICE:-kidspos}"
SERVICE_USER="${KIDSPOS_SERVICE_USER:-pi}"
REQUIRED_JAVA_MAJOR="${KIDSPOS_REQUIRED_JAVA_MAJOR:-21}"
UNIT_DIR="${KIDSPOS_UNIT_DIR:-/etc/systemd/system}"

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
UNIT_TEMPLATE="${SCRIPT_DIR}/kidspos.service"
UNIT_PATH="${UNIT_DIR}/${SERVICE}.service"
JAR_PATH="${APP_DIR}/${JAR_NAME}"

usage() {
    echo "Usage:"
    echo "  sudo $0                  初回セットアップを行い、GitHub Releases から最新の app.jar を導入する（要インターネット接続）"
    echo "  sudo $0 <path/to/jar>    初回セットアップを行い、手元に持ち込んだ jar を導入する（オフライン運用）"
    echo "  sudo $0 --no-jar         セットアップのみ行い、jar の導入は行わない"
    exit 1
}

log() { echo "[install] $*"; }
fail() { echo "[install] ERROR: $*" >&2; exit 1; }

LOCAL_JAR=""
SKIP_JAR=false
for arg in "$@"; do
    case "$arg" in
        --no-jar) SKIP_JAR=true ;;
        -h|--help) usage ;;
        -*)
            echo "[install] 不明なオプション: $arg" >&2
            usage
            ;;
        *) LOCAL_JAR="$arg" ;;
    esac
done

if [ -n "$LOCAL_JAR" ] && [ "$SKIP_JAR" = true ]; then
    fail "jar のパスと --no-jar は同時に指定できません"
fi
if [ -n "$LOCAL_JAR" ] && [ ! -f "$LOCAL_JAR" ]; then
    fail "ファイルが見つかりません: $LOCAL_JAR"
fi

command -v systemctl >/dev/null || fail "systemctl が必要です"
command -v java >/dev/null || fail "java が見つかりません。先に導入してください: sudo apt install -y openjdk-${REQUIRED_JAVA_MAJOR}-jre-headless"

JAVA_MAJOR=$(java -version 2>&1 | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | head -n1)
[ -n "$JAVA_MAJOR" ] || fail "java のバージョンを判定できませんでした: $(java -version 2>&1 | head -n1)"
[ "$JAVA_MAJOR" -ge "$REQUIRED_JAVA_MAJOR" ] ||
    fail "Java ${REQUIRED_JAVA_MAJOR} 以上が必要です（検出: $JAVA_MAJOR）。sudo apt install -y openjdk-${REQUIRED_JAVA_MAJOR}-jre-headless"

[ -f "$UNIT_TEMPLATE" ] || fail "systemd ユニットのテンプレートがありません: $UNIT_TEMPLATE"
[ -f "${SCRIPT_DIR}/update-app.sh" ] || fail "update-app.sh がありません: ${SCRIPT_DIR}/update-app.sh"

log "アプリケーションディレクトリを準備します: $APP_DIR"
mkdir -p "$APP_DIR" "${APP_DIR}/backup"

for script in update-app.sh doctor.sh; do
    if [ -f "${SCRIPT_DIR}/${script}" ]; then
        cp "${SCRIPT_DIR}/${script}" "${APP_DIR}/${script}"
        chmod +x "${APP_DIR}/${script}"
        log "配置しました: ${APP_DIR}/${script}"
    fi
done

TMP_UNIT=$(mktemp /var/tmp/kidspos-unit.XXXXXXXX)
trap 'rm -f "$TMP_UNIT"' EXIT
sed -e "s#/home/pi/kidspos/kidspos\.jar#${JAR_PATH}#g" \
    -e "s#/home/pi/kidspos#${APP_DIR}#g" \
    -e "s#^User=pi\$#User=${SERVICE_USER}#" \
    "$UNIT_TEMPLATE" > "$TMP_UNIT"

if cmp -s "$TMP_UNIT" "$UNIT_PATH" 2>/dev/null; then
    log "systemd ユニットは最新です: $UNIT_PATH"
else
    mkdir -p "$UNIT_DIR"
    cp "$TMP_UNIT" "$UNIT_PATH"
    log "systemd ユニットを配置しました: $UNIT_PATH"
fi

sudo systemctl daemon-reload
sudo systemctl enable "$SERVICE"
log "自動起動を有効にしました: $SERVICE"

if [ "$SKIP_JAR" = true ]; then
    log "--no-jar が指定されたため jar の導入は行いません"
elif [ -n "$LOCAL_JAR" ]; then
    log "持ち込んだ jar を導入します: $LOCAL_JAR"
    "${APP_DIR}/update-app.sh" "$LOCAL_JAR"
elif [ -f "$JAR_PATH" ]; then
    log "jar は既に配置済みのため導入をスキップします: $JAR_PATH"
    log "更新する場合: sudo ${APP_DIR}/update-app.sh"
    sudo systemctl start "$SERVICE" || true
else
    log "GitHub Releases から最新の app.jar を導入します"
    "${APP_DIR}/update-app.sh"
fi

if id "$SERVICE_USER" >/dev/null 2>&1; then
    chown -R "${SERVICE_USER}:$(id -gn "$SERVICE_USER")" "$APP_DIR"
fi

log "セットアップが完了しました"
if [ -x "${APP_DIR}/doctor.sh" ]; then
    echo ""
    "${APP_DIR}/doctor.sh" || true
fi
