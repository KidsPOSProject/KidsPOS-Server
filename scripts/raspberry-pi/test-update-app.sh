#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
UPDATE_SCRIPT="${SCRIPT_DIR}/update-app.sh"

PASS_COUNT=0
FAIL_COUNT=0
CURRENT_TEST=""

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

setup() {
    CURRENT_TEST="$1"
    echo "test: $CURRENT_TEST"
    WORK=$(mktemp -d)
    APP_DIR="${WORK}/app"
    STUB_DIR="${WORK}/bin"
    mkdir -p "$APP_DIR" "$STUB_DIR"

    export CALL_LOG="${WORK}/calls.log"
    export HEALTH_OK_FILE="${WORK}/health-ok"
    export FAIL_START_ONCE_FILE="${WORK}/fail-start-once"
    : > "$CALL_LOG"

    cat > "${STUB_DIR}/sudo" <<'EOF'
#!/usr/bin/env bash
exec "$@"
EOF

    cat > "${STUB_DIR}/systemctl" <<'EOF'
#!/usr/bin/env bash
echo "systemctl $*" >> "$CALL_LOG"
if [ "${1:-}" = "start" ] && [ -f "$FAIL_START_ONCE_FILE" ]; then
    rm -f "$FAIL_START_ONCE_FILE"
    exit 1
fi
exit 0
EOF

    cat > "${STUB_DIR}/curl" <<'EOF'
#!/usr/bin/env bash
echo "curl $*" >> "$CALL_LOG"
if [ -f "$HEALTH_OK_FILE" ]; then
    exit 0
fi
exit 22
EOF

    cat > "${STUB_DIR}/sleep" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF

    chmod +x "${STUB_DIR}"/*

    printf 'PK-old-jar' > "${APP_DIR}/kidspos.jar"
    printf 'old-db' > "${APP_DIR}/kidspos.db"
    printf 'PK-new-jar' > "${WORK}/new.jar"
}

teardown() {
    rm -rf "$WORK"
}

run_update() {
    set +e
    env PATH="${STUB_DIR}:${PATH}" \
        KIDSPOS_APP_DIR="$APP_DIR" \
        KIDSPOS_HEALTH_RETRIES=2 \
        bash "$UPDATE_SCRIPT" "$@" > "${WORK}/out.log" 2>&1
    RC=$?
    set -e
}

test_success_path() {
    setup "ローカル jar での更新が成功し、サービスの停止と起動が行われる"
    touch "$HEALTH_OK_FILE"
    run_update "${WORK}/new.jar"

    assert_eq 0 "$RC" "終了コードが 0"
    assert_eq "PK-new-jar" "$(cat "${APP_DIR}/kidspos.jar")" "jar が新しい内容に置き換わる"
    assert_contains "$CALL_LOG" "systemctl stop kidspos" "サービス停止が呼ばれる"
    assert_contains "$CALL_LOG" "systemctl start kidspos" "サービス起動が呼ばれる"
    if [ -f "${APP_DIR}/.installed-version" ]; then
        pass "バージョンファイルが書かれる"
    else
        fail_assert "バージョンファイルが書かれる"
    fi
    if ls "${APP_DIR}/backup/kidspos.jar."* >/dev/null 2>&1; then
        pass "jar のバックアップが作成される"
    else
        fail_assert "jar のバックアップが作成される"
    fi
    teardown
}

test_health_check_failure_rolls_back() {
    setup "ヘルスチェック失敗時に jar と DB が巻き戻される"
    run_update "${WORK}/new.jar"

    assert_eq 1 "$RC" "終了コードが 1"
    assert_eq "PK-old-jar" "$(cat "${APP_DIR}/kidspos.jar")" "jar が旧内容に復元される"
    assert_eq "old-db" "$(cat "${APP_DIR}/kidspos.db")" "DB が旧内容に復元される"
    assert_contains "${WORK}/out.log" "巻き戻しを実行します" "巻き戻しログが出力される"
    assert_contains "${WORK}/out.log" "旧バージョンに戻しました" "失敗メッセージが出力される"
    teardown
}

test_start_failure_rolls_back() {
    setup "サービス起動失敗時に ERR トラップ経由で巻き戻される"
    touch "$HEALTH_OK_FILE"
    touch "$FAIL_START_ONCE_FILE"
    run_update "${WORK}/new.jar"

    assert_eq 1 "$RC" "終了コードが 1"
    assert_eq "PK-old-jar" "$(cat "${APP_DIR}/kidspos.jar")" "jar が旧内容に復元される"
    assert_contains "${WORK}/out.log" "巻き戻しを実行します" "巻き戻しログが出力される"
    teardown
}

test_unknown_flag_is_rejected() {
    setup "不明なオプションは usage を表示して何も変更しない"
    touch "$HEALTH_OK_FILE"
    run_update --bogus

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "不明なオプション: --bogus" "不明オプションのエラーが出力される"
    assert_contains "${WORK}/out.log" "Usage:" "usage が表示される"
    assert_not_contains "$CALL_LOG" "systemctl" "サービスは操作されない"
    assert_eq "PK-old-jar" "$(cat "${APP_DIR}/kidspos.jar")" "jar は変更されない"
    teardown
}

test_non_jar_file_is_rejected() {
    setup "jar 形式でないファイルはサービス停止前に拒否される"
    touch "$HEALTH_OK_FILE"
    printf 'not-a-jar' > "${WORK}/bogus.bin"
    run_update "${WORK}/bogus.bin"

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "jar 形式ではありません" "形式エラーが出力される"
    assert_not_contains "$CALL_LOG" "systemctl" "サービスは操作されない"
    assert_eq "PK-old-jar" "$(cat "${APP_DIR}/kidspos.jar")" "jar は変更されない"
    teardown
}

test_success_path
test_health_check_failure_rolls_back
test_start_failure_rolls_back
test_unknown_flag_is_rejected
test_non_jar_file_is_rejected

echo ""
echo "passed: $PASS_COUNT, failed: $FAIL_COUNT"
[ "$FAIL_COUNT" -eq 0 ]
