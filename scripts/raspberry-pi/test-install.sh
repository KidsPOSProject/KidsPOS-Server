#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
INSTALL_SCRIPT="${SCRIPT_DIR}/install.sh"

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
    mkdir -p "$STUB_DIR" "$UNIT_DIR"

    export CALL_LOG="${WORK}/calls.log"
    export HEALTH_NG_FILE="${WORK}/health-ng"
    export STATE_DIR="${WORK}/state"
    mkdir -p "$STATE_DIR"
    : > "$CALL_LOG"
    echo "21.0.8" > "${STATE_DIR}/java-version"

    cat > "${STUB_DIR}/sudo" <<'EOF'
#!/usr/bin/env bash
exec "$@"
EOF

    cat > "${STUB_DIR}/java" <<'EOF'
#!/usr/bin/env bash
echo "openjdk version \"$(cat "${STATE_DIR}/java-version")\" 2025-07-15" >&2
exit 0
EOF

    cat > "${STUB_DIR}/systemctl" <<'EOF'
#!/usr/bin/env bash
echo "systemctl $*" >> "$CALL_LOG"
case "$1" in
    is-enabled) echo "enabled" ;;
    is-active) [ -f "${STATE_DIR}/active" ] || exit 3 ;;
    show)
        case "${3:-}" in
            NRestarts) echo "0" ;;
            User) id -un ;;
        esac
        ;;
esac
exit 0
EOF

    cat > "${STUB_DIR}/curl" <<'EOF'
#!/usr/bin/env bash
echo "curl $*" >> "$CALL_LOG"
for arg in "$@"; do
    case "$arg" in
        *api.github.com*) exit 22 ;;
    esac
done
if [ -f "$HEALTH_NG_FILE" ]; then
    exit 22
fi
exit 0
EOF

    cat > "${STUB_DIR}/sleep" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF

    chmod +x "${STUB_DIR}"/*

    printf 'PK\003\004' > "${WORK}/app.jar"
    printf 'BOOT-INF/classes/\n' >> "${WORK}/app.jar"
}

teardown() {
    rm -rf "$WORK"
}

run_install() {
    set +e
    env PATH="${STUB_DIR}:${PATH}" \
        KIDSPOS_APP_DIR="$APP_DIR" \
        KIDSPOS_UNIT_DIR="$UNIT_DIR" \
        KIDSPOS_SERVICE_USER="kidspostest" \
        KIDSPOS_HEALTH_RETRIES=2 \
        bash "$INSTALL_SCRIPT" "$@" > "${WORK}/out.log" 2>&1
    RC=$?
    set -e
}

test_install_with_local_jar() {
    setup "持ち込んだ jar でセットアップが完了する"
    run_install "${WORK}/app.jar"

    assert_eq 0 "$RC" "終了コードが 0"
    assert_file "${UNIT_DIR}/kidspos-server.service" "systemd ユニットが配置される"
    assert_file "${APP_DIR}/update-app.sh" "update-app.sh が配置される"
    assert_file "${APP_DIR}/doctor.sh" "doctor.sh が配置される"
    assert_file "${APP_DIR}/app.jar" "jar が配置される"
    assert_contains "$CALL_LOG" "systemctl daemon-reload" "daemon-reload が呼ばれる"
    assert_contains "$CALL_LOG" "systemctl enable kidspos-server" "自動起動が有効化される"
    assert_contains "$CALL_LOG" "systemctl start kidspos-server" "サービスが起動される"
    teardown
}

test_unit_paths_are_rewritten() {
    setup "ユニットの配置先とサービスユーザーが環境に合わせて書き換わる"
    run_install "${WORK}/app.jar"

    UNIT="${UNIT_DIR}/kidspos-server.service"
    assert_contains "$UNIT" "WorkingDirectory=${APP_DIR}$" "WorkingDirectory が置換される"
    assert_contains "$UNIT" "\-jar ${APP_DIR}/app.jar" "jar のパスが置換される"
    assert_contains "$UNIT" "^User=kidspostest$" "サービスユーザーが置換される"
    assert_not_contains "$UNIT" "/opt/kidspos" "既定パスが残らない"
    teardown
}

test_service_name_is_rewritten() {
    setup "サービス名を変えるとユニット名とログ識別子が追随する"
    set +e
    env PATH="${STUB_DIR}:${PATH}" \
        KIDSPOS_APP_DIR="$APP_DIR" \
        KIDSPOS_UNIT_DIR="$UNIT_DIR" \
        KIDSPOS_SERVICE="kidspos-alt" \
        KIDSPOS_SERVICE_USER="kidspostest" \
        KIDSPOS_HEALTH_RETRIES=2 \
        bash "$INSTALL_SCRIPT" "${WORK}/app.jar" > "${WORK}/out.log" 2>&1
    RC=$?
    set -e

    assert_eq 0 "$RC" "終了コードが 0"
    assert_file "${UNIT_DIR}/kidspos-alt.service" "サービス名のユニットが配置される"
    assert_contains "${UNIT_DIR}/kidspos-alt.service" "^SyslogIdentifier=kidspos-alt$" "ログ識別子が置換される"
    assert_contains "$CALL_LOG" "systemctl enable kidspos-alt" "サービス名で自動起動が有効化される"
    teardown
}

test_install_is_idempotent() {
    setup "同じ引数で再実行しても成功し設定が壊れない"
    run_install "${WORK}/app.jar"
    assert_eq 0 "$RC" "1 回目の終了コードが 0"
    FIRST_UNIT=$(cat "${UNIT_DIR}/kidspos-server.service")

    run_install "${WORK}/app.jar"
    assert_eq 0 "$RC" "2 回目の終了コードが 0"
    assert_eq "$FIRST_UNIT" "$(cat "${UNIT_DIR}/kidspos-server.service")" "ユニットの内容が変わらない"
    assert_contains "${WORK}/out.log" "systemd ユニットは最新です" "変更が無いことが報告される"
    teardown
}

test_existing_jar_is_kept() {
    setup "jar が既にある場合は入れ替えずサービスを起動する"
    mkdir -p "$APP_DIR"
    printf 'PK\003\004existing' > "${APP_DIR}/app.jar"
    run_install

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "既に配置済み" "スキップが報告される"
    assert_eq "PK"$'\003\004'"existing" "$(cat "${APP_DIR}/app.jar")" "jar が置き換わらない"
    assert_not_contains "$CALL_LOG" "systemctl stop" "更新用のサービス停止は行われない"
    assert_contains "$CALL_LOG" "systemctl start kidspos-server" "サービスが起動される"
    teardown
}

test_running_service_is_restarted_when_unit_changes() {
    setup "稼働中にユニットが書き換わったらサービスが再起動される"
    mkdir -p "$APP_DIR"
    printf 'PK\003\004existing' > "${APP_DIR}/app.jar"
    touch "${STATE_DIR}/active"
    run_install

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "ユニットの内容が変わったためサービスを再起動します" "再起動が報告される"
    assert_contains "$CALL_LOG" "systemctl restart kidspos-server" "restart が呼ばれる"
    teardown
}

test_running_service_is_not_restarted_when_unit_is_unchanged() {
    setup "ユニットに変更が無ければ稼働中のサービスは再起動されない"
    mkdir -p "$APP_DIR"
    printf 'PK\003\004existing' > "${APP_DIR}/app.jar"
    touch "${STATE_DIR}/active"
    run_install

    : > "$CALL_LOG"
    run_install

    assert_eq 0 "$RC" "2 回目の終了コードが 0"
    assert_contains "${WORK}/out.log" "systemd ユニットは最新です" "変更が無いことが報告される"
    assert_not_contains "$CALL_LOG" "systemctl restart" "restart は呼ばれない"
    assert_contains "$CALL_LOG" "systemctl start kidspos-server" "start だけが呼ばれる"
    teardown
}

test_no_jar_option() {
    setup "--no-jar ならセットアップのみ行い jar を導入しない"
    run_install --no-jar

    assert_eq 0 "$RC" "終了コードが 0"
    assert_file "${UNIT_DIR}/kidspos-server.service" "systemd ユニットが配置される"
    assert_no_path "${APP_DIR}/app.jar" "jar は導入されない"
    assert_contains "$CALL_LOG" "systemctl enable kidspos-server" "自動起動は有効化される"
    assert_not_contains "$CALL_LOG" "systemctl start" "jar が無いので起動はしない"
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

test_conflicting_options_are_rejected() {
    setup "jar のパスと --no-jar の同時指定は拒否される"
    run_install "${WORK}/app.jar" --no-jar

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "同時に指定できません" "競合が報告される"
    assert_no_path "$APP_DIR" "アプリケーションディレクトリは作られない"
    teardown
}

test_missing_jar_path_is_rejected() {
    setup "存在しない jar のパスは拒否される"
    run_install "${WORK}/missing.jar"

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "ファイルが見つかりません" "パスの誤りが報告される"
    assert_no_path "$APP_DIR" "アプリケーションディレクトリは作られない"
    teardown
}

test_old_java_is_rejected() {
    setup "Java 21 未満は導入方法を示して停止する"
    echo "17.0.9" > "${STATE_DIR}/java-version"
    run_install "${WORK}/app.jar"

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "Java 21 以上が必要です" "バージョン不足が報告される"
    assert_contains "${WORK}/out.log" "openjdk-21-jre-headless" "導入コマンドが示される"
    assert_no_path "$APP_DIR" "アプリケーションディレクトリは作られない"
    teardown
}

test_health_is_awaited_before_doctor() {
    setup "サービス起動後は応答を待ってから診断を行う"
    mkdir -p "$APP_DIR"
    printf 'PK\003\004existing' > "${APP_DIR}/app.jar"
    run_install

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "サービスの応答を待っています" "待機が報告される"
    assert_contains "${WORK}/out.log" "サービスが応答しました" "応答が報告される"
    assert_contains "$CALL_LOG" "curl -fsS --max-time 10 -o /dev/null http://localhost:8080/api/status" "ヘルスチェックがタイムアウト付きで呼ばれる"

    WAIT_LINE=$(grep -n "サービスの応答を待っています" "${WORK}/out.log" | head -n1 | cut -d: -f1)
    DOCTOR_LINE=$(grep -n "KidsPOS 稼働診断" "${WORK}/out.log" | head -n1 | cut -d: -f1)
    if [ -n "$WAIT_LINE" ] && [ -n "$DOCTOR_LINE" ] && [ "$WAIT_LINE" -lt "$DOCTOR_LINE" ]; then
        pass "待機が診断より先に行われる"
    else
        fail_assert "待機が診断より先に行われる (wait: ${WAIT_LINE:-none}, doctor: ${DOCTOR_LINE:-none})"
    fi
    teardown
}

test_health_wait_gives_up_and_continues() {
    setup "応答が無くても上限で待機を打ち切り診断へ進む"
    mkdir -p "$APP_DIR"
    printf 'PK\003\004existing' > "${APP_DIR}/app.jar"
    touch "$HEALTH_NG_FILE"
    run_install

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "待ち時間の上限に達しました" "打ち切りが報告される"
    assert_not_contains "${WORK}/out.log" "サービスが応答しました" "応答したとは報告されない"
    assert_contains "${WORK}/out.log" "KidsPOS 稼働診断" "診断が実行される"
    assert_eq 3 "$(grep -c "curl -fsS --max-time 10 -o /dev/null http://localhost:8080/api/status" "$CALL_LOG")" "待機は 2 回で打ち切られ診断の 1 回だけが続く"
    teardown
}

test_health_is_not_awaited_when_service_is_not_started() {
    setup "サービスを起動しない場合は応答を待たない"
    run_install --no-jar

    assert_eq 0 "$RC" "終了コードが 0"
    assert_not_contains "$CALL_LOG" "systemctl start" "サービスは起動されない"
    assert_not_contains "${WORK}/out.log" "サービスの応答を待っています" "待機は行われない"
    assert_contains "${WORK}/out.log" "KidsPOS 稼働診断" "診断は実行される"
    teardown
}

test_install_with_local_jar
test_unit_paths_are_rewritten
test_service_name_is_rewritten
test_install_is_idempotent
test_existing_jar_is_kept
test_running_service_is_restarted_when_unit_changes
test_running_service_is_not_restarted_when_unit_is_unchanged
test_no_jar_option
test_unknown_flag_is_rejected
test_conflicting_options_are_rejected
test_missing_jar_path_is_rejected
test_old_java_is_rejected
test_health_is_awaited_before_doctor
test_health_wait_gives_up_and_continues
test_health_is_not_awaited_when_service_is_not_started

echo ""
echo "passed: $PASS_COUNT, failed: $FAIL_COUNT"
[ "$FAIL_COUNT" -eq 0 ]
