# Java バージョン対応方法

## 問題

ビルドや実行時にJavaバージョンが合わない場合のエラー:

- `UnsupportedClassVersionError: has been compiled by a more recent version of the Java Runtime`
- `class file version 65.0` (Java 21) vs `class file version 61.0` (Java 17)

## 解決方法

JAVA_HOMEに適切なJavaバージョンをセットして実行する

### miseでインストールされたJavaを使用する場合

```bash
# Javaのパスを確認
mise which java
# 例: $HOME/.local/share/mise/installs/java/openjdk-21.0.2/bin/java

# JAVA_HOMEをセットして実行
export JAVA_HOME=$HOME/.local/share/mise/installs/java/openjdk-21.0.2 && ./gradlew bootRun
```

### プロジェクト要件

- **必要なJavaバージョン**: Java 21
- **ビルド時**: `./gradlew build -x detekt`
- **実行時**: `./gradlew bootRun`

### 注意事項

- プロジェクトはJava 21でコンパイルされているため、Java 21以降が必要
- miseでJava環境を管理している場合は、miseのJavaパスを使用する
