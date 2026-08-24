#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
UPDATE_SCRIPT="${SCRIPT_DIR}/update-app.sh"

PASS_COUNT=0
FAIL_COUNT=0
CURRENT_TEST=""
DISPLAY_MODULES="app.py config.py health.py layout.py renderer.py epaper.py"

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
    DISPLAY_DIR="${WORK}/display"
    mkdir -p "$APP_DIR" "$STUB_DIR" "$DISPLAY_DIR"

    export CALL_LOG="${WORK}/calls.log"
    export HEALTH_OK_FILE="${WORK}/health-ok"
    export FAIL_START_ONCE_FILE="${WORK}/fail-start-once"
    export RELEASE_JSON_FILE="${WORK}/release.json"
    export SCRIPTS_TARBALL_FILE="${WORK}/scripts.tar.gz"
    export SCRIPTS_DOWNLOAD_EXIT=0
    export UPLOAD_APK_EXIT=0
    export DISPLAY_INACTIVE_FILE="${WORK}/display-inactive"
    export DISPLAY_RESTART_EXIT=0
    : > "$CALL_LOG"

    DISPLAY_TARBALL_MODULES="$DISPLAY_MODULES"

    local module
    for module in $DISPLAY_MODULES; do
        printf '# old-display-marker\n' > "${DISPLAY_DIR}/${module}"
    done

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
if [ "${1:-}" = "is-active" ] && [ -f "$DISPLAY_INACTIVE_FILE" ]; then
    exit 3
fi
if [ "${1:-}" = "restart" ]; then
    exit "${DISPLAY_RESTART_EXIT:-0}"
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
    https://example.invalid/kidspos-scripts.tar.gz)
        [ "${SCRIPTS_DOWNLOAD_EXIT:-0}" = "0" ] || exit "${SCRIPTS_DOWNLOAD_EXIT}"
        OUT=""
        PREV=""
        for arg in "$@"; do
            [ "$PREV" = "-o" ] && OUT="$arg"
            PREV="$arg"
        done
        cp "$SCRIPTS_TARBALL_FILE" "$OUT"
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

write_release_json_with_scripts() {
    printf '[{"tag_name":"%s","draft":false,"prerelease":false,"assets":[{"name":"app.jar","browser_download_url":"https://example.invalid/app.jar"},{"name":"kidspos-scripts.tar.gz","browser_download_url":"https://example.invalid/kidspos-scripts.tar.gz"}]}]' \
        "$1" > "$RELEASE_JSON_FILE"
}

make_scripts_tarball() {
    local names=("$@")
    [ "${#names[@]}" -gt 0 ] || names=(update-app.sh doctor.sh upload-apk.sh)
    local src="${WORK}/scripts-src/raspberry-pi"
    rm -rf "${WORK}/scripts-src"
    mkdir -p "$src"
    local name
    for name in "${names[@]}"; do
        if [ "$name" = "upload-apk.sh" ]; then
            cat > "${src}/${name}" <<'EOF'
#!/usr/bin/env bash
echo "upload-apk-new $*" >> "$CALL_LOG"
exit 0
EOF
        else
            printf '#!/usr/bin/env bash\n# new-script-marker\n' > "${src}/${name}"
        fi
    done

    local entries=(raspberry-pi)
    if [ -n "$DISPLAY_TARBALL_MODULES" ]; then
        local display_src="${WORK}/scripts-src/raspberry-pi-display"
        mkdir -p "$display_src"
        local module
        for module in $DISPLAY_TARBALL_MODULES; do
            printf '# new-display-marker\n' > "${display_src}/${module}"
        done
        entries+=(raspberry-pi-display)
    fi

    tar -czf "$SCRIPTS_TARBALL_FILE" -C "${WORK}/scripts-src" "${entries[@]}"
}

assert_display_untouched() {
    local module
    for module in $DISPLAY_MODULES; do
        if grep -q "new-display-marker" "${DISPLAY_DIR}/${module}" 2>/dev/null; then
            fail_assert "$1"
            return
        fi
    done
    pass "$1"
}

assert_display_replaced() {
    local module
    for module in $DISPLAY_MODULES; do
        if ! grep -q "new-display-marker" "${DISPLAY_DIR}/${module}" 2>/dev/null; then
            fail_assert "$1 (未差し替え: ${module})"
            return
        fi
    done
    pass "$1"
}

assert_scripts_untouched() {
    if [ -e "${APP_DIR}/update-app.sh" ] || [ -e "${APP_DIR}/doctor.sh" ]; then
        fail_assert "$1"
    else
        pass "$1"
    fi
}

run_update() {
    set +e
    env PATH="${STUB_DIR}:${PATH}" \
        KIDSPOS_APP_DIR="$APP_DIR" \
        KIDSPOS_DISPLAY_DIR="$DISPLAY_DIR" \
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
        KIDSPOS_DISPLAY_DIR="$DISPLAY_DIR" \
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
        KIDSPOS_DISPLAY_DIR="$DISPLAY_DIR" \
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

test_self_update_replaces_scripts() {
    setup "リリースに同梱されたスクリプトで自身が差し替えられる"
    touch "$HEALTH_OK_FILE"
    write_release_json_with_scripts "v9.9.9"
    make_scripts_tarball
    install_upload_apk_stub
    run_update

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${APP_DIR}/update-app.sh" "new-script-marker" "update-app.sh が差し替わる"
    assert_contains "${APP_DIR}/doctor.sh" "new-script-marker" "doctor.sh が差し替わる"
    assert_contains "${APP_DIR}/upload-apk.sh" "upload-apk-new" "upload-apk.sh が差し替わる"
    assert_eq "v9.9.9" "$(cat "${APP_DIR}/.installed-scripts-version")" "スクリプトのバージョンが記録される"
    if [ -x "${APP_DIR}/update-app.sh" ]; then
        pass "差し替えたスクリプトに実行権限が付く"
    else
        fail_assert "差し替えたスクリプトに実行権限が付く"
    fi
    if [ -e "${APP_DIR}/.scripts-update" ]; then
        fail_assert "作業ディレクトリが後片付けされる"
    else
        pass "作業ディレクトリが後片付けされる"
    fi
    teardown
}

test_self_update_runs_before_apk_sync() {
    setup "APK の確認は差し替え後の upload-apk.sh で行われる"
    touch "$HEALTH_OK_FILE"
    write_release_json_with_scripts "v9.9.9"
    make_scripts_tarball
    install_upload_apk_stub
    run_update

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "$CALL_LOG" "upload-apk-new --server http://localhost:8080" "新しい APK 登録スクリプトが呼ばれる"
    teardown
}

test_self_update_when_jar_already_latest() {
    setup "jar が最新でもスクリプトが古ければ差し替えられる"
    touch "$HEALTH_OK_FILE"
    write_release_json_with_scripts "v9.9.9"
    make_scripts_tarball
    install_upload_apk_stub
    echo "v9.9.9" > "${APP_DIR}/.installed-version"
    run_update

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "すでに最新です" "最新である旨が出力される"
    assert_contains "${APP_DIR}/doctor.sh" "new-script-marker" "doctor.sh が差し替わる"
    assert_not_contains "$CALL_LOG" "kidspos-server" "サーバーのサービスは操作されない"
    teardown
}

test_self_update_skipped_when_scripts_version_matches() {
    setup "スクリプトが同じバージョンなら再取得しない"
    touch "$HEALTH_OK_FILE"
    write_release_json_with_scripts "v9.9.9"
    make_scripts_tarball
    install_upload_apk_stub
    echo "v9.9.9" > "${APP_DIR}/.installed-version"
    echo "v9.9.9" > "${APP_DIR}/.installed-scripts-version"
    run_update

    assert_eq 0 "$RC" "終了コードが 0"
    assert_not_contains "$CALL_LOG" "kidspos-scripts.tar.gz" "スクリプトはダウンロードされない"
    assert_contains "$CALL_LOG" "upload-apk --server http://localhost:8080" "既存の APK 登録スクリプトが呼ばれる"
    teardown
}

test_self_update_skipped_with_flag() {
    setup "--skip-self-update を付けるとスクリプトは差し替えられない"
    touch "$HEALTH_OK_FILE"
    write_release_json_with_scripts "v9.9.9"
    make_scripts_tarball
    run_update --skip-self-update

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "更新が完了しました" "サーバーの更新は完了する"
    assert_not_contains "$CALL_LOG" "kidspos-scripts.tar.gz" "スクリプトはダウンロードされない"
    assert_scripts_untouched "スクリプトは配置されない"
    teardown
}

test_self_update_skipped_when_asset_missing() {
    setup "リリースにスクリプトが無くても更新は成功する"
    touch "$HEALTH_OK_FILE"
    write_release_json "v9.9.9"
    run_update

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "kidspos-scripts.tar.gz が無いためスクリプトの更新は行いません" "スキップした旨が出力される"
    assert_contains "${WORK}/out.log" "更新が完了しました" "サーバーの更新は完了する"
    assert_scripts_untouched "スクリプトは配置されない"
    teardown
}

test_self_update_download_failure_does_not_fail_update() {
    setup "スクリプトの取得に失敗してもサーバーの更新は成功扱いになる"
    touch "$HEALTH_OK_FILE"
    write_release_json_with_scripts "v9.9.9"
    make_scripts_tarball
    export SCRIPTS_DOWNLOAD_EXIT=22
    run_update

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "スクリプトの取得に失敗しました" "警告が出力される"
    assert_contains "${WORK}/out.log" "更新が完了しました" "サーバーの更新は完了扱いになる"
    assert_scripts_untouched "スクリプトは配置されない"
    if [ -e "${APP_DIR}/.installed-scripts-version" ]; then
        fail_assert "スクリプトのバージョンは記録されない"
    else
        pass "スクリプトのバージョンは記録されない"
    fi
    teardown
}

test_self_update_extract_failure_does_not_fail_update() {
    setup "スクリプトの展開に失敗してもサーバーの更新は成功扱いになる"
    touch "$HEALTH_OK_FILE"
    write_release_json_with_scripts "v9.9.9"
    printf 'not-a-tarball' > "$SCRIPTS_TARBALL_FILE"
    run_update

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "スクリプトの展開に失敗しました" "警告が出力される"
    assert_contains "${WORK}/out.log" "更新が完了しました" "サーバーの更新は完了扱いになる"
    assert_scripts_untouched "スクリプトは配置されない"
    teardown
}

test_self_update_partial_tarball_is_not_recorded() {
    setup "配布物に足りないスクリプトがあればバージョンを記録しない"
    touch "$HEALTH_OK_FILE"
    write_release_json_with_scripts "v9.9.9"
    make_scripts_tarball update-app.sh upload-apk.sh
    run_update

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "配布物に含まれていません: doctor.sh" "不足が警告される"
    assert_contains "${APP_DIR}/update-app.sh" "new-script-marker" "含まれている分は差し替わる"
    if [ -e "${APP_DIR}/.installed-scripts-version" ]; then
        fail_assert "スクリプトのバージョンは記録されない"
    else
        pass "スクリプトのバージョンは記録されない"
    fi
    teardown
}

test_self_update_skipped_for_local_jar() {
    setup "持ち込んだ jar での更新ではスクリプトは差し替えられない"
    touch "$HEALTH_OK_FILE"
    run_update "${WORK}/new.jar"

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "更新が完了しました" "サーバーの更新は完了する"
    assert_scripts_untouched "スクリプトは配置されない"
    teardown
}

test_self_update_not_run_when_rolled_back() {
    setup "巻き戻しが起きたときはスクリプトは差し替えられない"
    write_release_json_with_scripts "v9.9.9"
    make_scripts_tarball
    run_update

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "巻き戻しを実行します" "巻き戻しログが出力される"
    assert_not_contains "$CALL_LOG" "kidspos-scripts.tar.gz" "スクリプトはダウンロードされない"
    assert_scripts_untouched "スクリプトは配置されない"
    teardown
}

test_display_update_replaces_modules() {
    setup "リリースに同梱された表示サービスのコードが差し替えられる"
    touch "$HEALTH_OK_FILE"
    write_release_json_with_scripts "v9.9.9"
    make_scripts_tarball
    run_update

    assert_eq 0 "$RC" "終了コードが 0"
    assert_display_replaced "表示サービスのコードが差し替わる"
    assert_contains "${WORK}/out.log" "表示サービスを更新しました" "更新した旨が出力される"
    assert_contains "$CALL_LOG" "systemctl restart kidspos-display" "表示サービスが再起動される"
    assert_eq "v9.9.9" "$(cat "${APP_DIR}/.installed-scripts-version")" "スクリプトのバージョンが記録される"
    if [ -x "${DISPLAY_DIR}/app.py" ]; then
        pass "app.py に実行権限が付く"
    else
        fail_assert "app.py に実行権限が付く"
    fi
    teardown
}

test_display_restarts_only_when_active() {
    setup "表示サービスが停止中なら再起動しない"
    touch "$HEALTH_OK_FILE"
    touch "$DISPLAY_INACTIVE_FILE"
    write_release_json_with_scripts "v9.9.9"
    make_scripts_tarball
    run_update

    assert_eq 0 "$RC" "終了コードが 0"
    assert_display_replaced "表示サービスのコードが差し替わる"
    assert_contains "${WORK}/out.log" "表示サービスは停止中のため再起動しません" "停止中である旨が出力される"
    assert_not_contains "$CALL_LOG" "systemctl restart kidspos-display" "再起動は呼ばれない"
    assert_eq "v9.9.9" "$(cat "${APP_DIR}/.installed-scripts-version")" "スクリプトのバージョンが記録される"
    teardown
}

test_display_skipped_when_dir_missing() {
    setup "表示サービスが導入されていなければ何もしない"
    touch "$HEALTH_OK_FILE"
    rm -rf "$DISPLAY_DIR"
    write_release_json_with_scripts "v9.9.9"
    make_scripts_tarball
    run_update

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "表示サービスが導入されていないため更新しません" "スキップした旨が出力される"
    assert_not_contains "$CALL_LOG" "systemctl restart kidspos-display" "再起動は呼ばれない"
    assert_eq "v9.9.9" "$(cat "${APP_DIR}/.installed-scripts-version")" "スクリプトのバージョンが記録される"
    if [ -d "$DISPLAY_DIR" ]; then
        fail_assert "配置先は作成されない"
    else
        pass "配置先は作成されない"
    fi
    teardown
}

test_display_skipped_with_flag() {
    setup "--skip-display を付けると表示サービスは差し替えられない"
    touch "$HEALTH_OK_FILE"
    write_release_json_with_scripts "v9.9.9"
    make_scripts_tarball
    run_update --skip-display

    assert_eq 0 "$RC" "終了コードが 0"
    assert_display_untouched "表示サービスのコードは差し替わらない"
    assert_contains "${APP_DIR}/doctor.sh" "new-script-marker" "スクリプトは差し替わる"
    assert_not_contains "$CALL_LOG" "systemctl restart kidspos-display" "再起動は呼ばれない"
    assert_eq "v9.9.9" "$(cat "${APP_DIR}/.installed-scripts-version")" "スクリプトのバージョンが記録される"
    teardown
}

test_display_partial_tarball_is_not_recorded() {
    setup "配布物に足りない表示モジュールがあればバージョンを記録しない"
    touch "$HEALTH_OK_FILE"
    write_release_json_with_scripts "v9.9.9"
    DISPLAY_TARBALL_MODULES="app.py config.py health.py renderer.py epaper.py"
    make_scripts_tarball
    run_update

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "配布物に含まれていません: raspberry-pi-display/layout.py" "不足が警告される"
    assert_contains "${DISPLAY_DIR}/app.py" "new-display-marker" "含まれている分は差し替わる"
    if [ -e "${APP_DIR}/.installed-scripts-version" ]; then
        fail_assert "スクリプトのバージョンは記録されない"
    else
        pass "スクリプトのバージョンは記録されない"
    fi
    teardown
}

test_display_missing_in_tarball() {
    setup "配布物に表示サービスが無ければバージョンを記録しない"
    touch "$HEALTH_OK_FILE"
    write_release_json_with_scripts "v9.9.9"
    DISPLAY_TARBALL_MODULES=""
    make_scripts_tarball
    run_update

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "配布物に raspberry-pi-display が含まれていません" "不足が警告される"
    assert_contains "${WORK}/out.log" "更新が完了しました" "サーバーの更新は完了扱いになる"
    assert_display_untouched "表示サービスのコードは差し替わらない"
    if [ -e "${APP_DIR}/.installed-scripts-version" ]; then
        fail_assert "スクリプトのバージョンは記録されない"
    else
        pass "スクリプトのバージョンは記録されない"
    fi
    teardown
}

test_display_restart_failure_does_not_fail_update() {
    setup "表示サービスの再起動に失敗してもサーバーの更新は成功扱いになる"
    touch "$HEALTH_OK_FILE"
    write_release_json_with_scripts "v9.9.9"
    make_scripts_tarball
    export DISPLAY_RESTART_EXIT=1
    run_update

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "表示サービスの再起動に失敗しました" "警告が出力される"
    assert_contains "${WORK}/out.log" "更新が完了しました" "サーバーの更新は完了扱いになる"
    if [ -e "${APP_DIR}/.installed-scripts-version" ]; then
        fail_assert "スクリプトのバージョンは記録されない"
    else
        pass "スクリプトのバージョンは記録されない"
    fi
    teardown
}

test_display_not_updated_for_local_jar() {
    setup "持ち込んだ jar での更新では表示サービスは差し替えられない"
    touch "$HEALTH_OK_FILE"
    run_update "${WORK}/new.jar"

    assert_eq 0 "$RC" "終了コードが 0"
    assert_display_untouched "表示サービスのコードは差し替わらない"
    assert_not_contains "$CALL_LOG" "systemctl restart kidspos-display" "再起動は呼ばれない"
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
test_self_update_replaces_scripts
test_self_update_runs_before_apk_sync
test_self_update_when_jar_already_latest
test_self_update_skipped_when_scripts_version_matches
test_self_update_skipped_with_flag
test_self_update_skipped_when_asset_missing
test_self_update_download_failure_does_not_fail_update
test_self_update_extract_failure_does_not_fail_update
test_self_update_partial_tarball_is_not_recorded
test_self_update_skipped_for_local_jar
test_self_update_not_run_when_rolled_back
test_display_update_replaces_modules
test_display_restarts_only_when_active
test_display_skipped_when_dir_missing
test_display_skipped_with_flag
test_display_partial_tarball_is_not_recorded
test_display_missing_in_tarball
test_display_restart_failure_does_not_fail_update
test_display_not_updated_for_local_jar

echo ""
echo "passed: $PASS_COUNT, failed: $FAIL_COUNT"
[ "$FAIL_COUNT" -eq 0 ]
