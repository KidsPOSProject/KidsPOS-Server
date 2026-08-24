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
    export RELEASE_JSON_FILE="${WORK}/release.json"
    export UPLOAD_APK_EXIT=0
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
URL="${@: -1}"
case "$URL" in
    https://api.github.com/*)
        cat "$RELEASE_JSON_FILE"
        exit 0
        ;;
    https://example.invalid/*)
        OUT=""
        PREV=""
        for arg in "$@"; do
            [ "$PREV" = "-o" ] && OUT="$arg"
            PREV="$arg"
        done
        printf 'PK-downloaded-jar' > "$OUT"
        exit 0
        ;;
esac
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

    printf 'PK-old-jar' > "${APP_DIR}/app.jar"
    printf 'old-db' > "${APP_DIR}/kidspos.db"
    printf 'PK-new-jar' > "${WORK}/new.jar"
}

teardown() {
    rm -rf "$WORK"
}

install_upload_apk_stub() {
    cat > "${APP_DIR}/upload-apk.sh" <<'EOF'
#!/usr/bin/env bash
echo "upload-apk $*" >> "$CALL_LOG"
exit "${UPLOAD_APK_EXIT:-0}"
EOF
    chmod +x "${APP_DIR}/upload-apk.sh"
}

write_release_json() {
    printf '[{"tag_name":"%s","draft":false,"prerelease":false,"assets":[{"name":"app.jar","browser_download_url":"https://example.invalid/app.jar"}]}]' \
        "$1" > "$RELEASE_JSON_FILE"
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
    assert_eq "PK-new-jar" "$(cat "${APP_DIR}/app.jar")" "jar が新しい内容に置き換わる"
    assert_contains "$CALL_LOG" "systemctl stop kidspos-server" "サービス停止が呼ばれる"
    assert_contains "$CALL_LOG" "systemctl start kidspos-server" "サービス起動が呼ばれる"
    if [ -f "${APP_DIR}/.installed-version" ]; then
        pass "バージョンファイルが書かれる"
    else
        fail_assert "バージョンファイルが書かれる"
    fi
    if ls "${APP_DIR}/backup/app.jar."* >/dev/null 2>&1; then
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
    assert_eq "PK-old-jar" "$(cat "${APP_DIR}/app.jar")" "jar が旧内容に復元される"
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
    assert_eq "PK-old-jar" "$(cat "${APP_DIR}/app.jar")" "jar が旧内容に復元される"
    assert_contains "${WORK}/out.log" "巻き戻しを実行します" "巻き戻しログが出力される"
    teardown
}

test_first_install_without_backups() {
    setup "jar も DB もバックアップも無い初回導入で成功する"
    touch "$HEALTH_OK_FILE"
    rm -f "${APP_DIR}/app.jar" "${APP_DIR}/kidspos.db"
    run_update "${WORK}/new.jar"

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "更新が完了しました" "完了メッセージが出力される"
    assert_eq "PK-new-jar" "$(cat "${APP_DIR}/app.jar")" "jar が配置される"
    if [ -f "${APP_DIR}/.installed-version" ]; then
        pass "バージョンファイルが書かれる"
    else
        fail_assert "バージョンファイルが書かれる"
    fi
    teardown
}

test_old_backups_are_pruned() {
    setup "バックアップは世代数を超えた分だけ削除される"
    touch "$HEALTH_OK_FILE"
    mkdir -p "${APP_DIR}/backup"
    for stamp in 20260101000001 20260101000002 20260101000003; do
        printf 'old' > "${APP_DIR}/backup/kidspos.db.${stamp}"
        printf 'old' > "${APP_DIR}/backup/app.jar.${stamp}"
    done

    set +e
    env PATH="${STUB_DIR}:${PATH}" \
        KIDSPOS_APP_DIR="$APP_DIR" \
        KIDSPOS_HEALTH_RETRIES=2 \
        KIDSPOS_BACKUP_KEEP=2 \
        bash "$UPDATE_SCRIPT" "${WORK}/new.jar" > "${WORK}/out.log" 2>&1
    RC=$?
    set -e

    assert_eq 0 "$RC" "終了コードが 0"
    assert_eq 2 "$(ls -1 "${APP_DIR}/backup/kidspos.db."* | wc -l | tr -d ' ')" "DB のバックアップが 2 世代に保たれる"
    assert_eq 2 "$(ls -1 "${APP_DIR}/backup/app.jar."* | wc -l | tr -d ' ')" "jar のバックアップが 2 世代に保たれる"
    if [ -e "${APP_DIR}/backup/kidspos.db.20260101000001" ]; then
        fail_assert "最も古いバックアップが削除される"
    else
        pass "最も古いバックアップが削除される"
    fi
    teardown
}

test_health_check_uses_timeout() {
    setup "ヘルスチェックはタイムアウト付きで呼ばれ環境変数で変更できる"
    touch "$HEALTH_OK_FILE"
    run_update "${WORK}/new.jar"

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "$CALL_LOG" "curl -fsS --max-time 10 -o /dev/null http://localhost:8080/api/status" "既定のタイムアウトが渡される"

    : > "$CALL_LOG"
    set +e
    env PATH="${STUB_DIR}:${PATH}" \
        KIDSPOS_APP_DIR="$APP_DIR" \
        KIDSPOS_HEALTH_RETRIES=2 \
        KIDSPOS_HEALTH_TIMEOUT=3 \
        bash "$UPDATE_SCRIPT" "${WORK}/new.jar" > "${WORK}/out.log" 2>&1
    RC=$?
    set -e

    assert_eq 0 "$RC" "タイムアウト指定時の終了コードが 0"
    assert_contains "$CALL_LOG" "curl -fsS --max-time 3 -o /dev/null http://localhost:8080/api/status" "環境変数でタイムアウトを変更できる"
    teardown
}

test_health_check_timeout_applies_while_retrying() {
    setup "応答が無い間もタイムアウト付きで繰り返し呼ばれる"
    run_update "${WORK}/new.jar"

    assert_eq 1 "$RC" "終了コードが 1"
    assert_eq 2 "$(grep -c "curl -fsS --max-time 10 -o /dev/null http://localhost:8080/api/status" "$CALL_LOG")" "リトライ回数の上限で打ち切られる"
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
    assert_eq "PK-old-jar" "$(cat "${APP_DIR}/app.jar")" "jar は変更されない"
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
    assert_eq "PK-old-jar" "$(cat "${APP_DIR}/app.jar")" "jar は変更されない"
    teardown
}

test_apk_sync_after_online_update() {
    setup "オンライン更新の成功後に APK の確認が行われる"
    touch "$HEALTH_OK_FILE"
    write_release_json "v9.9.9"
    install_upload_apk_stub
    run_update

    assert_eq 0 "$RC" "終了コードが 0"
    assert_eq "PK-downloaded-jar" "$(cat "${APP_DIR}/app.jar")" "取得した jar が配置される"
    assert_contains "$CALL_LOG" "upload-apk --server http://localhost:8080" "APK 登録スクリプトが呼ばれる"
    teardown
}

test_apk_sync_when_already_up_to_date() {
    setup "jar が最新でも APK の確認は行われる"
    touch "$HEALTH_OK_FILE"
    write_release_json "v9.9.9"
    install_upload_apk_stub
    echo "v9.9.9" > "${APP_DIR}/.installed-version"
    run_update

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "すでに最新です" "最新である旨が出力される"
    assert_contains "$CALL_LOG" "upload-apk --server http://localhost:8080" "APK 登録スクリプトが呼ばれる"
    assert_not_contains "$CALL_LOG" "systemctl" "サービスは操作されない"
    assert_eq "PK-old-jar" "$(cat "${APP_DIR}/app.jar")" "jar は差し替えられない"
    teardown
}

test_apk_sync_failure_does_not_fail_update() {
    setup "APK の登録に失敗してもサーバーの更新は成功扱いになる"
    touch "$HEALTH_OK_FILE"
    write_release_json "v9.9.9"
    install_upload_apk_stub
    export UPLOAD_APK_EXIT=1
    run_update

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "APK の更新に失敗しました" "警告が出力される"
    assert_contains "${WORK}/out.log" "更新が完了しました" "サーバーの更新は完了扱いになる"
    teardown
}

test_apk_sync_skipped_with_flag() {
    setup "--skip-apk を付けると APK の確認は行われない"
    touch "$HEALTH_OK_FILE"
    write_release_json "v9.9.9"
    install_upload_apk_stub
    run_update --skip-apk

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "更新が完了しました" "サーバーの更新は完了する"
    assert_not_contains "$CALL_LOG" "upload-apk" "APK 登録スクリプトは呼ばれない"
    teardown
}

test_apk_sync_skipped_for_local_jar() {
    setup "持ち込んだ jar での更新では APK の確認は行われない"
    touch "$HEALTH_OK_FILE"
    install_upload_apk_stub
    run_update "${WORK}/new.jar"

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "オフライン更新のため APK の確認は行いません" "スキップした旨が出力される"
    assert_not_contains "$CALL_LOG" "upload-apk" "APK 登録スクリプトは呼ばれない"
    teardown
}

test_apk_sync_skipped_when_script_missing() {
    setup "APK 登録スクリプトが無くても更新は成功する"
    touch "$HEALTH_OK_FILE"
    write_release_json "v9.9.9"
    run_update

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "APK 登録スクリプトが無いため" "スキップした旨が出力される"
    assert_contains "${WORK}/out.log" "更新が完了しました" "サーバーの更新は完了する"
    teardown
}

test_apk_sync_not_run_when_rolled_back() {
    setup "巻き戻しが起きたときは APK の確認は行われない"
    write_release_json "v9.9.9"
    install_upload_apk_stub
    run_update

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "巻き戻しを実行します" "巻き戻しログが出力される"
    assert_not_contains "$CALL_LOG" "upload-apk" "APK 登録スクリプトは呼ばれない"
    teardown
}

test_success_path
test_health_check_failure_rolls_back
test_start_failure_rolls_back
test_first_install_without_backups
test_old_backups_are_pruned
test_health_check_uses_timeout
test_health_check_timeout_applies_while_retrying
test_unknown_flag_is_rejected
test_non_jar_file_is_rejected
test_apk_sync_after_online_update
test_apk_sync_when_already_up_to_date
test_apk_sync_failure_does_not_fail_update
test_apk_sync_skipped_with_flag
test_apk_sync_skipped_for_local_jar
test_apk_sync_skipped_when_script_missing
test_apk_sync_not_run_when_rolled_back

echo ""
echo "passed: $PASS_COUNT, failed: $FAIL_COUNT"
[ "$FAIL_COUNT" -eq 0 ]
