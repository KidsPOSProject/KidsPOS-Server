#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
INSTALL_SCRIPT="${SCRIPT_DIR}/install-display.sh"

PASS_COUNT=0
FAIL_COUNT=0

pass() {
    PASS_COUNT=$((PASS_COUNT + 1))
    echo "  ok: $1"
}

fail_assert() {
    FAIL_COUNT=$((FAIL_COUNT + 1))
    echo "  NG: $1" >&2
}

assert_eq() {
    if [ "$1" = "$2" ]; then
        pass "$3"
    else
        fail_assert "$3 (expected: $1, actual: $2)"
    fi
}

assert_contains() {
    if grep -q "$2" "$1"; then
        pass "$3"
    else
        fail_assert "$3 (not found: $2)"
    fi
}

assert_not_contains() {
    if grep -q "$2" "$1" 2>/dev/null; then
        fail_assert "$3 (unexpectedly found: $2)"
    else
        pass "$3"
    fi
}

assert_file() {
    if [ -f "$1" ]; then
        pass "$2"
    else
        fail_assert "$2 (not a file: $1)"
    fi
}

assert_no_path() {
    if [ -e "$1" ]; then
        fail_assert "$2 (unexpectedly exists: $1)"
    else
        pass "$2"
    fi
}

setup() {
    echo "test: $1"
    WORK=$(mktemp -d)
    APP_DIR="${WORK}/app"
    STUB_DIR="${WORK}/bin"
    UNIT_DIR="${WORK}/systemd"
    SPI_DEVICE="${WORK}/spidev0.0"
    mkdir -p "$STUB_DIR" "$UNIT_DIR"
    touch "$SPI_DEVICE"

    export CALL_LOG="${WORK}/calls.log"
    export STATE_DIR="${WORK}/state"
    mkdir -p "$STATE_DIR"
    : > "$CALL_LOG"
    touch "${STATE_DIR}/py-PIL" "${STATE_DIR}/py-qrcode" "${STATE_DIR}/py-waveshare_epd"
    touch "${STATE_DIR}/user-exists"
    echo "spi gpio" > "${STATE_DIR}/system-groups"
    echo "kidspostest spi gpio" > "${STATE_DIR}/user-groups"

    cat > "${STUB_DIR}/sudo" <<'EOF'
#!/usr/bin/env bash
exec "$@"
EOF

    cat > "${STUB_DIR}/systemctl" <<'EOF'
#!/usr/bin/env bash
echo "systemctl $*" >> "$CALL_LOG"
case "$1" in
    is-active) [ -f "${STATE_DIR}/active" ] || exit 3 ;;
    is-enabled) echo "enabled" ;;
esac
exit 0
EOF

    cat > "${STUB_DIR}/python3" <<'EOF'
#!/usr/bin/env bash
echo "python3 $*" >> "$CALL_LOG"
if [ "${1:-}" = "-c" ]; then
    module=${2#import }
    [ -f "${STATE_DIR}/py-${module}" ] || exit 1
fi
exit 0
EOF

    cat > "${STUB_DIR}/id" <<'EOF'
#!/usr/bin/env bash
case "${1:-}" in
    -nG) cat "${STATE_DIR}/user-groups" ;;
    -gn|-un) echo "kidspostest" ;;
    *) [ -f "${STATE_DIR}/user-exists" ] || exit 1 ;;
esac
exit 0
EOF

    cat > "${STUB_DIR}/getent" <<'EOF'
#!/usr/bin/env bash
grep -qw "${2:-}" "${STATE_DIR}/system-groups"
EOF

    cat > "${STUB_DIR}/usermod" <<'EOF'
#!/usr/bin/env bash
echo "usermod $*" >> "$CALL_LOG"
exit 0
EOF

    cat > "${STUB_DIR}/chown" <<'EOF'
#!/usr/bin/env bash
echo "chown $*" >> "$CALL_LOG"
exit 0
EOF

    chmod +x "${STUB_DIR}"/*
}

teardown() {
    rm -rf "$WORK"
}

run_install() {
    set +e
    env PATH="${STUB_DIR}:${PATH}" \
        KIDSPOS_DISPLAY_APP_DIR="$APP_DIR" \
        KIDSPOS_DISPLAY_UNIT_DIR="$UNIT_DIR" \
        KIDSPOS_DISPLAY_SERVICE_USER="kidspostest" \
        KIDSPOS_DISPLAY_PYTHON="${STUB_DIR}/python3" \
        KIDSPOS_DISPLAY_SPI_DEVICE="$SPI_DEVICE" \
        bash "${SOURCE_SCRIPT:-$INSTALL_SCRIPT}" "$@" > "${WORK}/out.log" 2>&1
    RC=$?
    set -e
}

test_install_places_everything_and_starts() {
    setup "導入が完了しサービスが起動する"
    run_install

    assert_eq 0 "$RC" "終了コードが 0"
    assert_file "${UNIT_DIR}/kidspos-display.service" "systemd ユニットが配置される"
    assert_contains "$CALL_LOG" "systemctl daemon-reload" "daemon-reload が呼ばれる"
    assert_contains "$CALL_LOG" "systemctl enable kidspos-display" "自動起動が有効化される"
    assert_contains "$CALL_LOG" "systemctl start kidspos-display" "サービスが起動される"
    teardown
}

test_all_modules_are_copied() {
    setup "表示に必要な Python モジュールが配置される"
    run_install

    for module in app.py config.py health.py layout.py renderer.py epaper.py; do
        assert_file "${APP_DIR}/${module}" "${module} が配置される"
    done
    if [ -x "${APP_DIR}/app.py" ]; then
        pass "app.py に実行権限が付く"
    else
        fail_assert "app.py に実行権限が付く"
    fi
    teardown
}

test_unit_paths_are_rewritten() {
    setup "ユニットの配置先と実行ユーザーが環境に合わせて書き換わる"
    run_install

    UNIT="${UNIT_DIR}/kidspos-display.service"
    assert_contains "$UNIT" "WorkingDirectory=${APP_DIR}$" "WorkingDirectory が置換される"
    assert_contains "$UNIT" "ExecStart=${STUB_DIR}/python3 ${APP_DIR}/app.py" "ExecStart が置換される"
    assert_contains "$UNIT" "^User=kidspostest$" "実行ユーザーが置換される"
    assert_not_contains "$UNIT" "/opt/kidspos-display" "既定パスが残らない"
    teardown
}

test_service_name_is_rewritten() {
    setup "サービス名を変えるとユニット名とログ識別子が追随する"
    set +e
    env PATH="${STUB_DIR}:${PATH}" \
        KIDSPOS_DISPLAY_APP_DIR="$APP_DIR" \
        KIDSPOS_DISPLAY_UNIT_DIR="$UNIT_DIR" \
        KIDSPOS_DISPLAY_SERVICE="kidspos-panel" \
        KIDSPOS_DISPLAY_SERVICE_USER="kidspostest" \
        KIDSPOS_DISPLAY_PYTHON="${STUB_DIR}/python3" \
        KIDSPOS_DISPLAY_SPI_DEVICE="$SPI_DEVICE" \
        bash "$INSTALL_SCRIPT" > "${WORK}/out.log" 2>&1
    RC=$?
    set -e

    assert_eq 0 "$RC" "終了コードが 0"
    assert_file "${UNIT_DIR}/kidspos-panel.service" "サービス名のユニットが配置される"
    assert_contains "${UNIT_DIR}/kidspos-panel.service" "^SyslogIdentifier=kidspos-panel$" "ログ識別子が置換される"
    assert_contains "$CALL_LOG" "systemctl enable kidspos-panel" "サービス名で自動起動が有効化される"
    teardown
}

test_install_is_idempotent() {
    setup "再実行しても成功しユニットの内容が変わらない"
    run_install
    assert_eq 0 "$RC" "1 回目の終了コードが 0"
    FIRST_UNIT=$(cat "${UNIT_DIR}/kidspos-display.service")

    run_install
    assert_eq 0 "$RC" "2 回目の終了コードが 0"
    assert_eq "$FIRST_UNIT" "$(cat "${UNIT_DIR}/kidspos-display.service")" "ユニットの内容が変わらない"
    assert_contains "${WORK}/out.log" "systemd ユニットは最新です" "変更が無いことが報告される"
    teardown
}

test_running_service_is_restarted() {
    setup "稼働中に再実行すると新しいコードを読ませるため再起動される"
    run_install
    touch "${STATE_DIR}/active"
    : > "$CALL_LOG"
    run_install

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "$CALL_LOG" "systemctl restart kidspos-display" "restart が呼ばれる"
    assert_not_contains "$CALL_LOG" "systemctl start kidspos-display" "start は呼ばれない"
    teardown
}

test_no_start_option() {
    setup "--no-start なら導入のみ行いサービスを起動しない"
    run_install --no-start

    assert_eq 0 "$RC" "終了コードが 0"
    assert_file "${UNIT_DIR}/kidspos-display.service" "systemd ユニットが配置される"
    assert_file "${APP_DIR}/app.py" "モジュールは配置される"
    assert_contains "$CALL_LOG" "systemctl enable kidspos-display" "自動起動は有効化される"
    assert_not_contains "$CALL_LOG" "systemctl start" "サービスは起動されない"
    assert_not_contains "$CALL_LOG" "systemctl restart" "再起動もされない"
    teardown
}

test_missing_dependency_stops_with_guidance() {
    setup "Python の依存が足りなければ導入方法を示して停止する"
    rm -f "${STATE_DIR}/py-qrcode"
    run_install

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "Python の依存が足りません" "不足が報告される"
    assert_contains "${WORK}/out.log" "python3-qrcode" "導入コマンドが示される"
    assert_no_path "$APP_DIR" "アプリケーションディレクトリは作られない"
    assert_not_contains "$CALL_LOG" "systemctl" "systemd には触れない"
    teardown
}

test_skip_deps_option() {
    setup "--skip-deps なら依存が無くても導入できる"
    rm -f "${STATE_DIR}/py-PIL" "${STATE_DIR}/py-qrcode" "${STATE_DIR}/py-waveshare_epd"
    run_install --skip-deps

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "依存確認を省略します" "省略が報告される"
    assert_file "${UNIT_DIR}/kidspos-display.service" "systemd ユニットが配置される"
    teardown
}

test_missing_driver_only_warns() {
    setup "ドライバが無くても警告だけで導入は続く"
    rm -f "${STATE_DIR}/py-waveshare_epd"
    run_install

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "waveshare_epd ドライバを読み込めません" "警告が出る"
    assert_contains "${WORK}/out.log" "waveshareteam/e-Paper" "導入手順が示される"
    assert_file "${UNIT_DIR}/kidspos-display.service" "systemd ユニットが配置される"
    teardown
}

test_missing_spi_device_only_warns() {
    setup "SPI が有効でなくても警告だけで導入は続く"
    rm -f "$SPI_DEVICE"
    run_install

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "SPI デバイスがありません" "警告が出る"
    assert_contains "${WORK}/out.log" "raspi-config" "有効化の方法が示される"
    assert_file "${UNIT_DIR}/kidspos-display.service" "systemd ユニットが配置される"
    teardown
}

test_missing_group_is_added() {
    setup "SPI 用グループに未所属なら追加される"
    echo "kidspostest" > "${STATE_DIR}/user-groups"
    run_install

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "$CALL_LOG" "usermod -aG spi kidspostest" "spi グループが追加される"
    assert_contains "$CALL_LOG" "usermod -aG gpio kidspostest" "gpio グループが追加される"
    teardown
}

test_existing_group_is_not_touched() {
    setup "既に所属しているグループは追加し直さない"
    run_install

    assert_eq 0 "$RC" "終了コードが 0"
    assert_not_contains "$CALL_LOG" "usermod" "usermod は呼ばれない"
    teardown
}

test_unknown_group_is_skipped() {
    setup "システムに存在しないグループは追加を試みない"
    echo "spi" > "${STATE_DIR}/system-groups"
    echo "kidspostest" > "${STATE_DIR}/user-groups"
    run_install

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "$CALL_LOG" "usermod -aG spi kidspostest" "存在するグループは追加される"
    assert_not_contains "$CALL_LOG" "usermod -aG gpio" "存在しないグループは追加されない"
    teardown
}

test_missing_user_only_warns() {
    setup "実行ユーザーが居なければ警告するが導入は続く"
    rm -f "${STATE_DIR}/user-exists"
    run_install

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "ユーザーが存在しません" "警告が出る"
    assert_file "${UNIT_DIR}/kidspos-display.service" "systemd ユニットが配置される"
    teardown
}

test_unknown_flag_is_rejected() {
    setup "不明なオプションは usage を表示して何も作らない"
    run_install --bogus

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "不明なオプション: --bogus" "不明オプションのエラーが出力される"
    assert_contains "${WORK}/out.log" "Usage:" "usage が表示される"
    assert_no_path "$APP_DIR" "アプリケーションディレクトリは作られない"
    teardown
}

test_missing_module_is_rejected() {
    setup "配布物のモジュールが欠けていたら停止する"
    SOURCE_DIR="${WORK}/src"
    mkdir -p "$SOURCE_DIR"
    cp "${SCRIPT_DIR}/install-display.sh" "${SCRIPT_DIR}/kidspos-display.service" "$SOURCE_DIR"
    for module in app.py config.py health.py layout.py epaper.py; do
        cp "${SCRIPT_DIR}/${module}" "$SOURCE_DIR"
    done
    SOURCE_SCRIPT="${SOURCE_DIR}/install-display.sh" run_install

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "ファイルがありません" "欠落が報告される"
    assert_contains "${WORK}/out.log" "renderer.py" "欠けたファイル名が示される"
    assert_no_path "$APP_DIR" "アプリケーションディレクトリは作られない"
    teardown
}

test_install_places_everything_and_starts
test_all_modules_are_copied
test_unit_paths_are_rewritten
test_service_name_is_rewritten
test_install_is_idempotent
test_running_service_is_restarted
test_no_start_option
test_missing_dependency_stops_with_guidance
test_skip_deps_option
test_missing_driver_only_warns
test_missing_spi_device_only_warns
test_missing_group_is_added
test_existing_group_is_not_touched
test_unknown_group_is_skipped
test_missing_user_only_warns
test_unknown_flag_is_rejected
test_missing_module_is_rejected

echo ""
echo "passed: $PASS_COUNT, failed: $FAIL_COUNT"
[ "$FAIL_COUNT" -eq 0 ]
