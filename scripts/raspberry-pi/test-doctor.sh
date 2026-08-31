#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
DOCTOR_SCRIPT="${SCRIPT_DIR}/doctor.sh"

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

setup() {
    echo "test: $1"
    WORK=$(mktemp -d)
    APP_DIR="${WORK}/app"
    STUB_DIR="${WORK}/bin"
    UNIT_DIR="${WORK}/systemd"
    mkdir -p "${APP_DIR}/backup" "${APP_DIR}/uploads" "$STUB_DIR" "$UNIT_DIR"

    export STATE_DIR="${WORK}/state"
    mkdir -p "$STATE_DIR"
    echo "enabled" > "${STATE_DIR}/is-enabled"
    echo "0" > "${STATE_DIR}/nrestarts"
    echo "yes" > "${STATE_DIR}/ntp"
    echo "cap_sys_time" > "${STATE_DIR}/ambient-capabilities"
    echo "21.0.8" > "${STATE_DIR}/java-version"
    touch "${STATE_DIR}/active"
    touch "${STATE_DIR}/health-ok"
    touch "${STATE_DIR}/port-listening"

    cat > "${STUB_DIR}/java" <<'EOF'
#!/usr/bin/env bash
echo "openjdk version \"$(cat "${STATE_DIR}/java-version")\" 2025-07-15" >&2
exit 0
EOF

    cat > "${STUB_DIR}/systemctl" <<'EOF'
#!/usr/bin/env bash
case "$1" in
    is-enabled) cat "${STATE_DIR}/is-enabled" ;;
    is-active) [ -f "${STATE_DIR}/active" ] || exit 3 ;;
    show)
        case "$2" in
            -p) case "$3" in
                    NRestarts) cat "${STATE_DIR}/nrestarts" ;;
                    User) id -un ;;
                    AmbientCapabilities) cat "${STATE_DIR}/ambient-capabilities" ;;
                esac ;;
        esac
        ;;
esac
exit 0
EOF

    cat > "${STUB_DIR}/curl" <<'EOF'
#!/usr/bin/env bash
for arg in "$@"; do
    case "$arg" in
        *api.github.com*)
            if [ -f "${STATE_DIR}/release-json" ]; then
                cat "${STATE_DIR}/release-json"
                exit 0
            fi
            exit 22
            ;;
    esac
done
[ -f "${STATE_DIR}/health-ok" ] || exit 22
exit 0
EOF

    cat > "${STUB_DIR}/ss" <<'EOF'
#!/usr/bin/env bash
if [ -f "${STATE_DIR}/port-listening" ]; then
    echo "LISTEN 0 100 *:8080 *:*"
fi
exit 0
EOF

    cat > "${STUB_DIR}/timedatectl" <<'EOF'
#!/usr/bin/env bash
cat "${STATE_DIR}/ntp"
exit 0
EOF

    cat > "${STUB_DIR}/fake-hwclock" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF

    cat > "${STUB_DIR}/journalctl" <<'EOF'
#!/usr/bin/env bash
[ -f "${STATE_DIR}/journal" ] && cat "${STATE_DIR}/journal"
exit 0
EOF

    chmod +x "${STUB_DIR}"/*

    write_boot_jar "${APP_DIR}/app.jar"
    printf 'db-content' > "${APP_DIR}/kidspos.db"
    printf 'v1.0.0' > "${APP_DIR}/.installed-version"
    printf 'PK-old' > "${APP_DIR}/backup/kidspos.db.20260101000000"
    write_unit "${UNIT_DIR}/kidspos-server.service"

    FAKE_HWCLOCK_DATA="${WORK}/fake-hwclock.data"
    printf 'Sat Aug 22 10:00:00 UTC 2026\n' > "$FAKE_HWCLOCK_DATA"
}

write_unit() {
    cat > "$1" <<EOF
[Service]
ExecStart=/usr/bin/java -jar ${APP_DIR}/app.jar
EOF
}

write_boot_jar() {
    printf 'PK\003\004' > "$1"
    printf 'BOOT-INF/classes/\n' >> "$1"
}

write_plain_jar() {
    printf 'PK\003\004' > "$1"
    printf 'info/nukoneko/kidspos/\n' >> "$1"
}

teardown() {
    rm -rf "$WORK"
}

run_doctor() {
    set +e
    env PATH="${STUB_DIR}:${PATH}" \
        KIDSPOS_APP_DIR="$APP_DIR" \
        KIDSPOS_UNIT_DIR="$UNIT_DIR" \
        KIDSPOS_FAKE_HWCLOCK_DATA="$FAKE_HWCLOCK_DATA" \
        bash "$DOCTOR_SCRIPT" > "${WORK}/out.log" 2>&1
    RC=$?
    set -e
}

test_healthy_environment() {
    setup "健全な環境ではすべて OK になり終了コードが 0"
    run_doctor

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "NG 0" "NG が 0 件"
    assert_contains "${WORK}/out.log" "Java 21" "Java のバージョンが表示される"
    assert_contains "${WORK}/out.log" "jar は実行可能な形式です" "jar が実行可能と判定される"
    assert_contains "${WORK}/out.log" "サービスは起動しています" "サービス起動が検出される"
    assert_contains "${WORK}/out.log" "ポート 8080 が待ち受け中です" "待ち受けポートが検出される"
    assert_contains "${WORK}/out.log" "時刻が同期されています" "時刻同期が検出される"
    assert_contains "${WORK}/out.log" "同じ jar を起動する別のユニットはありません" "多重起動が無いことが報告される"
    assert_contains "${WORK}/out.log" "アップロード領域があります" "アップロード領域が検出される"
    teardown
}

test_missing_jar_is_ng() {
    setup "jar が無い場合は NG になる"
    rm "${APP_DIR}/app.jar"
    run_doctor

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "jar がありません" "jar 欠落が報告される"
    assert_contains "${WORK}/out.log" "update-app.sh" "対処コマンドが示される"
    teardown
}

test_plain_jar_is_ng() {
    setup "plain jar が置かれている場合は NG になる"
    write_plain_jar "${APP_DIR}/app.jar"
    run_doctor

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "実行可能な jar ではありません" "plain jar が検出される"
    teardown
}

test_broken_jar_is_ng() {
    setup "zip 形式でないファイルが置かれている場合は NG になる"
    printf 'not-a-jar' > "${APP_DIR}/app.jar"
    run_doctor

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "jar が壊れています" "壊れた jar が検出される"
    teardown
}

test_stopped_service_is_ng() {
    setup "サービス停止時は NG になり起動方法が示される"
    rm "${STATE_DIR}/active"
    rm "${STATE_DIR}/health-ok"
    run_doctor

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "サービスが停止しています" "サービス停止が報告される"
    assert_contains "${WORK}/out.log" "応答がありません" "ヘルスチェック失敗が報告される"
    assert_contains "${WORK}/out.log" "journalctl" "ログ確認コマンドが示される"
    teardown
}

test_disabled_autostart_is_warning() {
    setup "自動起動が無効なら注意止まりで終了コードは 0"
    echo "disabled" > "${STATE_DIR}/is-enabled"
    run_doctor

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "自動起動が無効です" "自動起動無効が報告される"
    assert_contains "${WORK}/out.log" "systemctl enable" "有効化コマンドが示される"
    teardown
}

test_restart_loop_is_warning() {
    setup "再起動を繰り返している場合は注意になる"
    echo "5" > "${STATE_DIR}/nrestarts"
    run_doctor

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "5 回再起動しています" "再起動回数が報告される"
    teardown
}

test_unsynced_clock_is_not_a_failure() {
    setup "時刻同期サーバー未同期はイントラネット運用では失敗にしない"
    echo "no" > "${STATE_DIR}/ntp"
    run_doctor

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "イントラネット運用では正常です" "オフライン運用の説明が出る"
    assert_not_contains "${WORK}/out.log" "\[注意\] 時刻" "注意としては報告されない"
    teardown
}

test_missing_time_capability_is_ng() {
    setup "サービスに時刻変更の権限が無ければ NG になる"
    echo "" > "${STATE_DIR}/ambient-capabilities"
    run_doctor

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "サービスに時刻変更の権限がありません" "権限不足が報告される"
    assert_contains "${WORK}/out.log" "update-app.sh" "対処が示される"
    teardown
}

test_time_capability_is_ok() {
    setup "サービスに時刻変更の権限があれば OK になる"
    run_doctor

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "サービスに時刻変更の権限があります" "権限があると報告される"
    teardown
}

test_missing_fake_hwclock_is_warning() {
    setup "fake-hwclock が無ければ注意になる"
    rm "${STUB_DIR}/fake-hwclock"
    run_doctor

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "fake-hwclock がありません" "未導入が報告される"
    teardown
}

test_missing_fake_hwclock_data_is_warning() {
    setup "fake-hwclock の記録が無ければ注意になる"
    rm "$FAKE_HWCLOCK_DATA"
    run_doctor

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "fake-hwclock の記録がありません" "記録欠落が報告される"
    teardown
}

test_fake_hwclock_data_is_reported() {
    setup "fake-hwclock の記録があれば内容が表示される"
    run_doctor

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "Sat Aug 22 10:00:00 UTC 2026" "記録された時刻が表示される"
    teardown
}

test_missing_version_file_is_warning() {
    setup "導入バージョン未記録は注意になる"
    rm "${APP_DIR}/.installed-version"
    run_doctor

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "導入バージョンが記録されていません" "バージョン未記録が報告される"
    teardown
}

test_newer_release_is_warning() {
    setup "より新しいリリースがある場合は注意になる"
    cat > "${STATE_DIR}/release-json" <<'EOF'
[{"tag_name":"v2.0.0","draft":false,"prerelease":false,"assets":[{"name":"app.jar"}]}]
EOF
    run_doctor

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "より新しいリリースがあります: v2.0.0" "新しいリリースが報告される"
    teardown
}

test_same_release_is_ok() {
    setup "最新リリースと同一なら OK になる"
    cat > "${STATE_DIR}/release-json" <<'EOF'
[{"tag_name":"v1.0.0","draft":false,"prerelease":false,"assets":[{"name":"app.jar"}]}]
EOF
    run_doctor

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "最新リリースと同じバージョンです" "最新であることが報告される"
    assert_not_contains "${WORK}/out.log" "より新しいリリースがあります" "更新の注意は出ない"
    teardown
}

test_offline_release_check_is_not_a_failure() {
    setup "リリース確認に失敗してもオフラインとして扱い NG にしない"
    run_doctor

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "最新リリースは確認できませんでした" "オフライン時の案内が出る"
    teardown
}

test_missing_app_dir_is_ng() {
    setup "アプリケーションディレクトリが無い場合は NG になる"
    rm -rf "$APP_DIR"
    run_doctor

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "アプリケーションディレクトリがありません" "ディレクトリ欠落が報告される"
    teardown
}

test_journal_error_is_warning() {
    setup "journal に例外があれば注意になる"
    printf 'java.lang.IllegalStateException: boom\n' > "${STATE_DIR}/journal"
    run_doctor

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "件のエラーがあります" "journal のエラーが報告される"
    teardown
}

test_clean_journal_is_ok() {
    setup "journal にエラーが無ければ OK になる"
    printf 'Started KidsPOS Server.\n' > "${STATE_DIR}/journal"
    run_doctor

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "直近のログに異常はありません" "ログが正常と報告される"
    assert_not_contains "${WORK}/out.log" "件のエラーがあります" "エラーの注意は出ない"
    teardown
}

test_duplicate_unit_is_ng() {
    setup "同じ jar を起動する別ユニットがあれば NG になる"
    write_unit "${UNIT_DIR}/kidspos.service"
    run_doctor

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "同じ jar を起動するユニットが他にもあります: kidspos" "多重起動が報告される"
    assert_contains "${WORK}/out.log" "systemctl disable --now kidspos" "停止コマンドが示される"
    teardown
}

test_unrelated_unit_is_not_reported() {
    setup "別の jar を起動するユニットは多重起動として扱わない"
    cat > "${UNIT_DIR}/kidspos-monitor.service" <<EOF
[Service]
ExecStart=/usr/bin/python3 ${APP_DIR}/monitor.py
EOF
    run_doctor

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "同じ jar を起動する別のユニットはありません" "誤検知しない"
    teardown
}

test_missing_uploads_is_warning() {
    setup "アップロード領域が無ければ注意になる"
    rm -rf "${APP_DIR}/uploads"
    run_doctor

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "アップロード領域がありません" "アップロード領域の欠落が報告される"
    teardown
}

test_healthy_environment
test_missing_jar_is_ng
test_plain_jar_is_ng
test_broken_jar_is_ng
test_stopped_service_is_ng
test_disabled_autostart_is_warning
test_restart_loop_is_warning
test_unsynced_clock_is_not_a_failure
test_missing_time_capability_is_ng
test_time_capability_is_ok
test_missing_fake_hwclock_is_warning
test_missing_fake_hwclock_data_is_warning
test_fake_hwclock_data_is_reported
test_missing_version_file_is_warning
test_newer_release_is_warning
test_same_release_is_ok
test_offline_release_check_is_not_a_failure
test_missing_app_dir_is_ng
test_journal_error_is_warning
test_clean_journal_is_ok
test_duplicate_unit_is_ng
test_unrelated_unit_is_not_reported
test_missing_uploads_is_warning

echo ""
echo "passed: $PASS_COUNT, failed: $FAIL_COUNT"
[ "$FAIL_COUNT" -eq 0 ]
