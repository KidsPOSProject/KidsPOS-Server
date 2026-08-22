#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="${KIDSPOS_DISPLAY_APP_DIR:-/opt/kidspos-display}"
SERVICE="${KIDSPOS_DISPLAY_SERVICE:-kidspos-display}"
SERVICE_USER="${KIDSPOS_DISPLAY_SERVICE_USER:-pi}"
UNIT_DIR="${KIDSPOS_DISPLAY_UNIT_DIR:-/etc/systemd/system}"
PYTHON_BIN="${KIDSPOS_DISPLAY_PYTHON:-/usr/bin/python3}"
SPI_DEVICE="${KIDSPOS_DISPLAY_SPI_DEVICE:-/dev/spidev0.0}"
DEVICE_GROUPS="${KIDSPOS_DISPLAY_GROUPS:-spi gpio}"

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
UNIT_TEMPLATE="${SCRIPT_DIR}/kidspos-display.service"
UNIT_PATH="${UNIT_DIR}/${SERVICE}.service"

MODULES="app.py config.py health.py layout.py renderer.py epaper.py"

usage() {
    echo "Usage:"
    echo "  sudo $0                  e-Paper 表示サービスを導入して起動する"
    echo "  sudo $0 --no-start       導入だけ行い、サービスは起動しない"
    echo "  sudo $0 --skip-deps      Python の依存確認を省略する"
    exit 1
}

log() { echo "[install-display] $*"; }
warn() { echo "[install-display] WARN: $*" >&2; }
fail() { echo "[install-display] ERROR: $*" >&2; exit 1; }

NO_START=false
SKIP_DEPS=false
for arg in "$@"; do
    case "$arg" in
        --no-start) NO_START=true ;;
        --skip-deps) SKIP_DEPS=true ;;
        -h|--help) usage ;;
        *)
            echo "[install-display] 不明なオプション: $arg" >&2
            usage
            ;;
    esac
done

command -v systemctl >/dev/null || fail "systemctl が必要です"
[ -x "$PYTHON_BIN" ] || fail "python3 が見つかりません: $PYTHON_BIN"
[ -f "$UNIT_TEMPLATE" ] || fail "systemd ユニットのテンプレートがありません: $UNIT_TEMPLATE"

for module in $MODULES; do
    [ -f "${SCRIPT_DIR}/${module}" ] || fail "ファイルがありません: ${SCRIPT_DIR}/${module}"
done

# 依存は自動導入しない。導入元が混在すると復旧が難しくなるため、手順の提示にとどめる
check_dependencies() {
    local missing=""
    "$PYTHON_BIN" -c "import PIL" 2>/dev/null || missing="${missing} pillow"
    "$PYTHON_BIN" -c "import qrcode" 2>/dev/null || missing="${missing} qrcode"
    [ -n "$missing" ] || return 0

    echo "[install-display] ERROR: Python の依存が足りません:${missing}" >&2
    echo "  sudo apt install -y python3-pil python3-qrcode" >&2
    echo "  導入後にもう一度このスクリプトを実行してください（確認を省く場合は --skip-deps）" >&2
    exit 1
}

check_driver() {
    "$PYTHON_BIN" -c "import waveshare_epd" 2>/dev/null && return 0
    warn "waveshare_epd ドライバを読み込めません。表示は失敗し続けます"
    echo "  Waveshare の e-Paper ライブラリを導入してください:" >&2
    echo "    git clone https://github.com/waveshareteam/e-Paper.git" >&2
    echo "    sudo cp -r e-Paper/RaspberryPi_JetsonNano/python/lib/waveshare_epd \\" >&2
    echo "      \"\$(${PYTHON_BIN} -c 'import site; print(site.getsitepackages()[0])')\"" >&2
}

if [ "$SKIP_DEPS" = true ]; then
    log "--skip-deps が指定されたため依存確認を省略します"
else
    check_dependencies
    check_driver
fi

[ -e "$SPI_DEVICE" ] || warn "SPI デバイスがありません: ${SPI_DEVICE}。sudo raspi-config で SPI を有効にしてください"

log "アプリケーションディレクトリを準備します: $APP_DIR"
mkdir -p "$APP_DIR"
for module in $MODULES; do
    cp "${SCRIPT_DIR}/${module}" "${APP_DIR}/${module}"
done
chmod +x "${APP_DIR}/app.py"
log "配置しました: ${MODULES}"

if id "$SERVICE_USER" >/dev/null 2>&1; then
    for group in $DEVICE_GROUPS; do
        getent group "$group" >/dev/null || continue
        id -nG "$SERVICE_USER" | tr ' ' '\n' | grep -qx "$group" && continue
        usermod -aG "$group" "$SERVICE_USER"
        log "${SERVICE_USER} を ${group} グループに追加しました"
    done
    chown -R "${SERVICE_USER}:$(id -gn "$SERVICE_USER")" "$APP_DIR"
else
    warn "ユーザーが存在しません: ${SERVICE_USER}。サービスは起動できません"
fi

TMP_UNIT=$(mktemp /var/tmp/kidspos-display-unit.XXXXXXXX)
trap 'rm -f "$TMP_UNIT"' EXIT
sed -e "s#/usr/bin/python3#${PYTHON_BIN}#g" \
    -e "s#/opt/kidspos-display#${APP_DIR}#g" \
    -e "s#^User=pi\$#User=${SERVICE_USER}#" \
    -e "s#^SyslogIdentifier=kidspos-display\$#SyslogIdentifier=${SERVICE}#" \
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

if [ "$NO_START" = true ]; then
    log "--no-start が指定されたためサービスは起動しません"
    log "起動する場合: sudo systemctl start ${SERVICE}"
    exit 0
fi

# 配置し直したコードを読ませるため、ユニットに変更が無くても稼働中なら再起動する
if systemctl is-active --quiet "$SERVICE"; then
    sudo systemctl restart "$SERVICE"
else
    sudo systemctl start "$SERVICE"
fi

if systemctl is-active --quiet "$SERVICE"; then
    log "サービスが起動しました: $SERVICE"
else
    warn "サービスが起動していません。ログを確認してください: journalctl -u ${SERVICE} -n 50"
fi

log "セットアップが完了しました"
log "ログ: journalctl -u ${SERVICE} -f"
