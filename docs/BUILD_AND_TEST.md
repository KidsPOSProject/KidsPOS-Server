# ビルドとテストガイド

## ビルドコマンド

### 通常ビルド
```bash
./gradlew build -x detekt
```

### クリーンビルド
```bash
./gradlew clean build -x detekt
```

### テストのみ実行
```bash
./gradlew test
```

## 重要なルール

**ビルド確認ルール**: タスク完了時は必ずビルド（`./gradlew build -x detekt`）とアプリケーション起動（`./gradlew bootRun`）を確認すること

## Detekt について

現在、Kotlin 2.2.20とDetekt 1.23.7の互換性問題があるため、ビルド時は `-x detekt` オプションを使用してDetektをスキップしています。

## テスト実行時の注意

- PDFテキスト抽出で日本語文字のエンコーディング問題があるため、一部のテストで通貨表示の検証がコメントアウトされています
- 全てのテストは `./gradlew test` で実行可能です

## Java環境

- **必須**: Java 21
- **環境変数**: `JAVA_HOME` をJava 21のインストールディレクトリに設定

## ビルド成果物

- JARファイル: `build/libs/`
- テストレポート: `build/reports/tests/test/index.html`
- Jacocoカバレッジレポート: `build/reports/jacoco/test/html/index.html`