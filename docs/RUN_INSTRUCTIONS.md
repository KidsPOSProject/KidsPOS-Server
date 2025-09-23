# KidsPOS Server 起動手順

## 起動方法

### 方法1: 起動スクリプト（推奨）
ポート8080を使用している既存プロセスを自動的に終了してから起動します。

```bash
./run.sh
```

### 方法2: Gradleタスク
```bash
./gradlew runWithPortClean
```

### 方法3: IntelliJ IDEA
IntelliJ IDEAの実行構成から「KidsPOS Run Script」を選択して実行

### 方法4: 手動でポートをクリア
```bash
# ポート8080のプロセスを終了
lsof -ti:8080 | xargs kill -9

# アプリケーション起動
./gradlew bootRun
```

## トラブルシューティング

### ポート8080が使用中のエラー
```
Web server failed to start. Port 8080 was already in use.
```

このエラーが表示された場合は、上記の方法1または方法2を使用してください。

### Java環境の設定
Java 21が必要です。環境変数が設定されていない場合は：

```bash
export JAVA_HOME=/Users/atsumi/.local/share/mise/installs/java/openjdk-21.0.2
```

## 開発時の注意事項

- 起動スクリプト（run.sh）は自動的に前のプロセスを終了します
- IntelliJから起動する場合も、実行構成で自動クリーンアップが可能です
- ビルドとテストは `./gradlew build -x detekt` で実行できます