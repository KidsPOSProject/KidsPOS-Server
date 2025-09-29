# APKダウンロード機能

KidsPOSサーバーにAPKダウンロード機能を追加しました。この機能により、アプリから最新バージョンのAPKをダウンロード・インストールできます。

## 機能概要

### 1. APKバージョン管理
- Web UIからAPKファイルをアップロード
- バージョンとバージョンコードによる管理
- リリースノートの記載
- 複数バージョンの管理と有効/無効の切り替え

### 2. APIエンドポイント

#### バージョン確認
```bash
# 最新バージョン情報の取得
GET /api/apk/version/latest

# アップデート確認
GET /api/apk/version/check?currentVersionCode=100

# 全バージョン一覧
GET /api/apk/version/all
```

#### ダウンロード
```bash
# 最新APKのダウンロード
GET /api/apk/download/latest

# 特定バージョンのダウンロード
GET /api/apk/download/{id}
```

#### 管理用API
```bash
# APKアップロード（管理者用）
POST /api/apk/upload
Content-Type: multipart/form-data
- file: APKファイル
- version: バージョン名（例: 1.2.3）
- versionCode: バージョンコード（整数）
- releaseNotes: リリースノート（任意）

# バージョン無効化
PUT /api/apk/version/{id}/deactivate

# バージョン削除
DELETE /api/apk/version/{id}
```

### 3. Web UI

#### APK管理画面
- アクセス: http://localhost:8080/apk
- 機能:
  - APKファイルのアップロード
  - バージョン一覧の表示
  - ダウンロードリンクの提供
  - バージョンの無効化/削除

## セットアップ

### 1. データベース更新
```bash
# Flywayマイグレーションが自動的に実行されます
./gradlew bootRun
```

### 2. HTTPSの設定（推奨）

APKダウンロード機能を本番環境で使用する場合は、HTTPSを有効にすることを推奨します。

#### 自己署名証明書の作成（開発環境用）
```bash
# 証明書作成スクリプトを実行
./create-ssl-cert.sh
```

#### HTTPS有効化
```bash
# 環境変数で有効化
export SSL_ENABLED=true
./gradlew bootRun

# または起動時に指定
SSL_ENABLED=true ./gradlew bootRun
```

#### 本番環境用の証明書
本番環境では、Let's Encryptなどの正式な証明書を使用してください:
```bash
# 正式な証明書を使用
export SSL_ENABLED=true
export SSL_KEY_STORE=/path/to/your/certificate.p12
export SSL_KEY_STORE_PASSWORD=your-password
export SSL_KEY_ALIAS=your-alias
```

### 3. APKアップロードディレクトリ

デフォルトでは `./uploads/apk` にAPKファイルが保存されます。変更する場合:
```bash
export APK_UPLOAD_DIR=/path/to/apk/directory
```

## アプリ側の実装例

### Kotlinでの実装例
```kotlin
// 最新バージョンの確認
suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? {
    val response = httpClient.get("https://server-address/api/apk/version/check?currentVersionCode=$currentVersionCode")
    return if (response.hasUpdate) {
        UpdateInfo(
            version = response.latestVersion.version,
            downloadUrl = response.latestVersion.downloadUrl,
            releaseNotes = response.latestVersion.releaseNotes
        )
    } else null
}

// APKダウンロード
suspend fun downloadApk(): File {
    val url = "https://server-address/api/apk/download/latest"
    // ダウンロード処理を実装
}

// インストール
fun installApk(context: Context, apkFile: File) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(
            FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile),
            "application/vnd.android.package-archive"
        )
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
```

## セキュリティ考慮事項

1. **HTTPS必須**: 本番環境では必ずHTTPSを使用してください
2. **認証**: 必要に応じてAPI認証を実装してください
3. **署名検証**: アプリ側でAPKの署名を検証することを推奨
4. **アップロード制限**: 管理者のみがAPKをアップロードできるように制限

## トラブルシューティング

### 証明書エラーが発生する場合
開発環境で自己署名証明書を使用している場合、以下の対処が必要です:

#### Android側の設定
`res/xml/network_security_config.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">192.168.1.100</domain>
        <trust-anchors>
            <certificates src="user" />
        </trust-anchors>
    </domain-config>
</network-security-config>
```

`AndroidManifest.xml`:
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

### アップロードサイズ制限
デフォルトは100MB。変更する場合:
```bash
export APK_MAX_FILE_SIZE=209715200  # 200MB
```

## 今後の改善点

- [ ] APKの自動署名検証
- [ ] 差分アップデート対応
- [ ] ロールバック機能
- [ ] ダウンロード統計
- [ ] チャンネル別配信（alpha, beta, stable）