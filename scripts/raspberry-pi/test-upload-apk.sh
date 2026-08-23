#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
UPLOAD_SCRIPT="${SCRIPT_DIR}/upload-apk.sh"

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
    if grep -q -e "$2" "$1"; then
        pass "$3"
    else
        fail_assert "$3 (not found: $2)"
    fi
}

assert_not_contains() {
    if grep -q -e "$2" "$1" 2>/dev/null; then
        fail_assert "$3 (unexpectedly found: $2)"
    else
        pass "$3"
    fi
}

setup() {
    CURRENT_TEST="$1"
    echo "test: $CURRENT_TEST"
    WORK=$(mktemp -d)
    STUB_DIR="${WORK}/bin"
    mkdir -p "$STUB_DIR"

    export CALL_LOG="${WORK}/calls.log"
    export RELEASES_JSON="${WORK}/releases.json"
    export UPLOAD_STATUS_FILE="${WORK}/upload-status"
    export UPLOAD_BODY_FILE="${WORK}/upload-body"
    export DOWNLOAD_CONTENT_FILE="${WORK}/download-content"
    export CURL_CONNECT_FAIL_FILE="${WORK}/curl-connect-fail"
    : > "$CALL_LOG"

    # curl の呼び出しは 3 系統ある。GitHub API 一覧（標準出力に JSON）、アセットのダウンロード（-o に保存）、
    # サーバーへの登録（-o に本文、標準出力に HTTP コード）。引数から系統を判別して固定の応答を返す
    cat > "${STUB_DIR}/curl" <<'EOF'
#!/usr/bin/env bash
echo "curl $*" >> "$CALL_LOG"

if [ -f "$CURL_CONNECT_FAIL_FILE" ] && [[ "$*" == *"/api/apk/upload"* ]]; then
    echo "curl: (7) Failed to connect" >&2
    exit 7
fi

OUT=""
PREV=""
for arg in "$@"; do
    if [ "$PREV" = "-o" ]; then
        OUT="$arg"
    fi
    PREV="$arg"
done

if [[ "$*" == *"api.github.com"* ]]; then
    cat "$RELEASES_JSON"
    exit 0
fi

if [[ "$*" == *"/api/apk/upload"* ]]; then
    cat "$UPLOAD_BODY_FILE" > "$OUT"
    cat "$UPLOAD_STATUS_FILE"
    exit 0
fi

cat "$DOWNLOAD_CONTENT_FILE" > "$OUT"
exit 0
EOF

    chmod +x "${STUB_DIR}"/*

    printf 'PK-apk-body' > "${WORK}/app.apk"
    printf 'PK-downloaded-apk' > "$DOWNLOAD_CONTENT_FILE"
    printf '201' > "$UPLOAD_STATUS_FILE"
    cat > "$UPLOAD_BODY_FILE" <<'EOF'
{"id":3,"version":"1.0.11","versionCode":11,"fileName":"kidspos-1.0.11.apk","fileSize":100,"releaseNotes":null,"downloadUrl":"/api/apk/download/3","uploadedAt":"2026-01-01T00:00:00","isActive":true}
EOF
    cat > "$RELEASES_JSON" <<'EOF'
[
  {"tag_name":"v1.0.12","draft":true,"prerelease":false,
   "assets":[{"name":"kidspos-v1.0.12.apk","browser_download_url":"https://example.invalid/draft.apk"}]},
  {"tag_name":"v1.0.11","draft":false,"prerelease":false,"body":"バーコード対応\nを修正",
   "assets":[{"name":"mapping.txt","browser_download_url":"https://example.invalid/mapping.txt"},
             {"name":"kidspos-v1.0.11.apk","browser_download_url":"https://example.invalid/kidspos-v1.0.11.apk"}]}
]
EOF
}

teardown() {
    rm -rf "$WORK"
}

count_temp_dirs() {
    local count=0
    for dir in /var/tmp/kidspos-upload.*; do
        [ -d "$dir" ] && count=$((count + 1))
    done
    echo "$count"
}

run_upload() {
    set +e
    env PATH="${STUB_DIR}:${PATH}" \
        bash "$UPLOAD_SCRIPT" "$@" > "${WORK}/out.log" 2>&1
    RC=$?
    set -e
}

test_local_file_upload() {
    setup "ローカル APK を指定すると GitHub を見ずにアップロードされる"
    run_upload "${WORK}/app.apk"

    assert_eq 0 "$RC" "終了コードが 0"
    assert_not_contains "$CALL_LOG" "api.github.com" "GitHub API は呼ばれない"
    assert_contains "$CALL_LOG" "file=@${WORK}/app.apk" "指定したファイルが送信される"
    assert_contains "$CALL_LOG" "http://localhost:8080/api/apk/upload" "既定のサーバーへ送信される"
    assert_contains "${WORK}/out.log" "登録しました: 1.0.11 (versionCode=11)" "登録されたバージョンが表示される"
    teardown
}

test_local_upload_without_notes() {
    setup "リリースノート未指定のローカル登録では releaseNotes を送らない"
    run_upload "${WORK}/app.apk"

    assert_eq 0 "$RC" "終了コードが 0"
    assert_not_contains "$CALL_LOG" "releaseNotes" "releaseNotes は付与されない"
    teardown
}

test_notes_option_is_sent() {
    setup "--notes で指定したリリースノートが送信される"
    run_upload --notes "手動アップロード" "${WORK}/app.apk"

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "$CALL_LOG" "releaseNotes=手動アップロード" "リリースノートが送信される"
    teardown
}

test_server_option_is_used() {
    setup "--server で送信先を変更でき、末尾のスラッシュは取り除かれる"
    run_upload --server "http://192.168.100.9:8080/" "${WORK}/app.apk"

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "$CALL_LOG" "http://192.168.100.9:8080/api/apk/upload" "指定したサーバーへ送信される"
    teardown
}

test_github_latest_release_is_downloaded() {
    setup "引数なしでは最新リリースの APK を取得して登録する"
    run_upload

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "$CALL_LOG" "api.github.com/repos/KidsPOSProject/KidsPOS-for-Android/releases" "既定のリポジトリを参照する"
    assert_contains "$CALL_LOG" "https://example.invalid/kidspos-v1.0.11.apk" "apk アセットがダウンロードされる"
    assert_not_contains "$CALL_LOG" "mapping.txt" "apk 以外のアセットは選ばれない"
    assert_not_contains "$CALL_LOG" "draft.apk" "draft のリリースは選ばれない"
    assert_contains "$CALL_LOG" "kidspos-v1.0.11.apk" "ダウンロードしたファイルが送信される"
    assert_contains "$CALL_LOG" "releaseNotes=v1.0.11 バーコード対応 を修正" "タグと本文がリリースノートになる"
    teardown
}

test_github_release_notes_are_overridden_by_option() {
    setup "オンライン取得でも --notes の指定が優先される"
    run_upload --notes "年1メンテナンスで更新"

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "$CALL_LOG" "releaseNotes=年1メンテナンスで更新" "指定したリリースノートが送信される"
    assert_not_contains "$CALL_LOG" "releaseNotes=v1.0.11" "リリース本文は使われない"
    teardown
}

test_no_apk_asset_is_rejected() {
    setup "apk アセットを含むリリースが無い場合は送信せず失敗する"
    cat > "$RELEASES_JSON" <<'EOF'
[{"tag_name":"v1.0.11","draft":false,"prerelease":false,"body":"","assets":[{"name":"mapping.txt","browser_download_url":"https://example.invalid/mapping.txt"}]}]
EOF
    run_upload

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "APK を含むリリースが見つかりません" "リリース不在のエラーが出力される"
    assert_not_contains "$CALL_LOG" "/api/apk/upload" "サーバーへは送信されない"
    teardown
}

test_non_apk_file_is_rejected() {
    setup "APK 形式でないファイルは送信前に拒否される"
    printf 'not-an-apk' > "${WORK}/bogus.bin"
    run_upload "${WORK}/bogus.bin"

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "APK 形式ではありません" "形式エラーが出力される"
    assert_not_contains "$CALL_LOG" "/api/apk/upload" "サーバーへは送信されない"
    teardown
}

test_missing_file_is_rejected() {
    setup "存在しないファイルを指定すると失敗する"
    run_upload "${WORK}/not-exist.apk"

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "ファイルが見つかりません" "ファイル不在のエラーが出力される"
    assert_not_contains "$CALL_LOG" "/api/apk/upload" "サーバーへは送信されない"
    teardown
}

test_duplicate_version_is_not_an_error() {
    setup "同じバージョンが登録済み（409）でも成功として終了する"
    printf '409' > "$UPLOAD_STATUS_FILE"
    cat > "$UPLOAD_BODY_FILE" <<'EOF'
{"code":"DUPLICATE_RESOURCE","message":"バージョン 1.0.11 は既に存在します","path":"uri=/api/apk/upload"}
EOF
    run_upload "${WORK}/app.apk"

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "${WORK}/out.log" "登録済みのため何もしませんでした" "登録済みの案内が出力される"
    assert_contains "${WORK}/out.log" "バージョン 1.0.11 は既に存在します" "サーバーのメッセージが表示される"
    teardown
}

test_validation_error_fails() {
    setup "バリデーションエラー（400）は失敗として終了する"
    printf '400' > "$UPLOAD_STATUS_FILE"
    cat > "$UPLOAD_BODY_FILE" <<'EOF'
{"code":"VALIDATION_ERROR","message":"APKとして解析できません","path":"uri=/api/apk/upload"}
EOF
    run_upload "${WORK}/app.apk"

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "APKとして解析できません" "サーバーのメッセージが表示される"
    assert_contains "${WORK}/out.log" "登録に失敗しました（HTTP 400）" "HTTP コード付きで失敗が報告される"
    teardown
}

test_connection_failure_fails() {
    setup "サーバーに接続できない場合は失敗として終了する"
    touch "$CURL_CONNECT_FAIL_FILE"
    run_upload "${WORK}/app.apk"

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "サーバーに接続できませんでした" "接続エラーが出力される"
    teardown
}

test_unknown_flag_is_rejected() {
    setup "不明なオプションは usage を表示して何もしない"
    run_upload --bogus

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "不明なオプション: --bogus" "不明オプションのエラーが出力される"
    assert_contains "${WORK}/out.log" "Usage:" "usage が表示される"
    assert_not_contains "$CALL_LOG" "curl" "curl は呼ばれない"
    teardown
}

test_option_without_value_is_rejected() {
    setup "値を伴わない --server は拒否される"
    run_upload --server

    assert_eq 1 "$RC" "終了コードが 1"
    assert_contains "${WORK}/out.log" "--server には URL を指定してください" "値不足のエラーが出力される"
    assert_not_contains "$CALL_LOG" "curl" "curl は呼ばれない"
    teardown
}

test_repo_can_be_overridden() {
    setup "参照するリポジトリは環境変数で変更できる"
    set +e
    env PATH="${STUB_DIR}:${PATH}" \
        KIDSPOS_ANDROID_REPO="example/other-app" \
        bash "$UPLOAD_SCRIPT" > "${WORK}/out.log" 2>&1
    RC=$?
    set -e

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "$CALL_LOG" "api.github.com/repos/example/other-app/releases" "指定したリポジトリを参照する"
    teardown
}

test_server_url_can_be_overridden_by_env() {
    setup "送信先サーバーは環境変数で変更できる"
    set +e
    env PATH="${STUB_DIR}:${PATH}" \
        KIDSPOS_SERVER_URL="http://kidspos.local:9090" \
        bash "$UPLOAD_SCRIPT" "${WORK}/app.apk" > "${WORK}/out.log" 2>&1
    RC=$?
    set -e

    assert_eq 0 "$RC" "終了コードが 0"
    assert_contains "$CALL_LOG" "http://kidspos.local:9090/api/apk/upload" "指定したサーバーへ送信される"
    teardown
}

test_temp_files_are_cleaned_up() {
    setup "一時ディレクトリは終了時に削除される"
    BEFORE=$(count_temp_dirs)
    run_upload
    AFTER=$(count_temp_dirs)

    assert_eq 0 "$RC" "終了コードが 0"
    assert_eq "$BEFORE" "$AFTER" "一時ディレクトリが残らない"
    teardown
}

test_local_file_upload
test_local_upload_without_notes
test_notes_option_is_sent
test_server_option_is_used
test_github_latest_release_is_downloaded
test_github_release_notes_are_overridden_by_option
test_no_apk_asset_is_rejected
test_non_apk_file_is_rejected
test_missing_file_is_rejected
test_duplicate_version_is_not_an_error
test_validation_error_fails
test_connection_failure_fails
test_unknown_flag_is_rejected
test_option_without_value_is_rejected
test_repo_can_be_overridden
test_server_url_can_be_overridden_by_env
test_temp_files_are_cleaned_up

echo ""
echo "passed: $PASS_COUNT, failed: $FAIL_COUNT"
[ "$FAIL_COUNT" -eq 0 ]
