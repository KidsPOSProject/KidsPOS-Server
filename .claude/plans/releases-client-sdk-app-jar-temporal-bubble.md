# Releases に app.jar を追加でアップロードする

## Context

現在、GitHub Releases には generate-sdk.yml ワークフロー（api.yaml が main に push されると発火）が client SDK の zip のみをアセットとして公開している。ユーザーはこのリリースに、サーバーの実行可能 jar（app.jar）も一緒に吐き出してほしい。

前提となる調査結果:

- リリース作成は .github/workflows/generate-sdk.yml の "Create GitHub Release with SDK asset" ステップ（87〜123行付近）で、curl + GitHub REST API により行われている（RELEASE_ID を取得後、uploads.github.com へアセットを POST）
- 実行可能 jar は `./gradlew bootJar` で `build/libs/server-1.0.0.jar` に出力される（`server-1.0.0-plain.jar` という非実行の plain jar も同時に生成される点に注意）
- Gradle の既存 stage タスクは `tasks.jar.archiveFile`（= plain jar）を app.jar にコピーする実装で、`java -jar app.jar` が動かない jar を作ってしまうため、ワークフローでは stage を使わない
- generate-sdk ジョブには既に JDK 21 セットアップ（cache: gradle）と gradlew の実行実績があるため、bootJar 追加のコストは低い
- `permissions: contents: write` と GITHUB_TOKEN は設定済みで権限変更は不要

## 変更内容

対象ファイル: .github/workflows/generate-sdk.yml（編集のみ、1ファイル）

1. "Generate Kotlin SDK" 〜 "Archive SDK" ステップの後（リリース作成ステップの前）に、app.jar をビルドするステップを追加する
   - `./gradlew bootJar -x test -x detekt` を実行（テストは build-test.yml が担っているため CI 時間短縮のためスキップ。release-build.yml と同じ流儀）
   - `build/libs/` から plain を除外して bootJar 出力を特定し、`app.jar` という名前でコピーする（バージョンのハードコードを避けるため release-build.yml の `ls server-*.jar | grep -v plain` パターンを流用）

2. "Create GitHub Release with SDK asset" ステップ内で、SDK zip のアップロードに続けて app.jar を同じ RELEASE_ID にアップロードする curl を追加する
   - `https://uploads.github.com/repos/${GITHUB_REPOSITORY}/releases/${RELEASE_ID}/assets?name=app.jar`
   - `Content-Type: application/java-archive`
   - RELEASE_ID はステップ内のシェル変数のため、同一ステップ内に追記する

3. リリース本文（body）に app.jar が含まれる旨を 1 行追記する

build.gradle は変更しない（stage タスクの plain jar 問題は既知の別課題として今回のスコープ外。報告のみ）。

## 実行手順

1. ブランチ claude/app-jar-release-output-m2pekn を作成（origin の default branch 起点）
2. generate-sdk.yml を上記の通り編集
3. 検証（下記）
4. コミット（変更ファイルは generate-sdk.yml のみを個別 add）
5. `git push -u origin claude/app-jar-release-output-m2pekn`（ネットワークエラー時は 2s/4s/8s/16s で最大4回リトライ)

PR 作成は指示されていないため行わない。

## 検証

- ローカルで `./gradlew bootJar -x test -x detekt` を実行し、build/libs/ に server-*.jar（plain でない方）が生成されることを確認
- plain 除外の glob で正しいファイルが 1 件選ばれることをシェルで確認し、app.jar へのコピーと `java -jar app.jar` の起動可否（起動ログが出ることまで）を確認
- ワークフロー YAML の構文チェック（yq または python -c 'import yaml' で parse）
- 実際のリリース動作は api.yaml 変更の main への push または workflow_dispatch でしか発火しないため、CI 上の最終確認はマージ後に workflow_dispatch で行う想定（今回のスコープ外）

## 期待結果

api.yaml 更新で SDK リリースが作られる際、同じリリースに app.jar（Spring Boot 実行可能 jar）が添付されるようになる。
