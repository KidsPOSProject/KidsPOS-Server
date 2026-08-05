# API Documentation

## OpenAPI仕様

OpenAPI仕様書は `/api.yaml` に定義されています。

## 通貨単位

このプロジェクトでは通貨単位として「リバー」を使用します。
- 表示形式: `150リバー`
- JSONレスポンス: `{"price": 150}` （単位は暗黙的にリバー）

## エンドポイント一覧

### Items API (/api/item)
- GET `/api/item` - 商品一覧取得
- POST `/api/item` - 商品登録
- GET `/api/item/{id}` - 商品取得
- PUT `/api/item/{id}` - 商品更新
- PATCH `/api/item/{id}` - 商品部分更新
- DELETE `/api/item/{id}` - 商品削除
- GET `/api/item/barcode/{barcode}` - バーコードで商品取得
- GET `/api/item/barcode-pdf` - バーコードPDF生成

### Sales API (/api/sales)
- GET `/api/sales` - 売上一覧取得
- POST `/api/sales` - 売上登録
- GET `/api/sales/{id}` - 売上詳細取得
- GET `/api/sales/validate-printer/{storeId}` - プリンター設定確認

### Staff API (/api/staff)
- GET `/api/staff` - スタッフ一覧取得
- POST `/api/staff` - スタッフ登録
- GET `/api/staff/{barcode}` - スタッフ取得
- PUT `/api/staff/{barcode}` - スタッフ更新
- DELETE `/api/staff/{barcode}` - スタッフ削除

### Stores API (/api/stores)
- GET `/api/stores` - 店舗一覧取得
- POST `/api/stores` - 店舗登録
- GET `/api/stores/{id}` - 店舗取得
- PUT `/api/stores/{id}` - 店舗更新
- DELETE `/api/stores/{id}` - 店舗削除

### Status API (/api/status)
- GET `/api/status` - サーバーステータス・バージョン情報取得（status / version / apiVersion を返却）

### Settings API (/api/setting)
- GET `/api/setting` - 設定一覧取得
- POST `/api/setting` - 設定作成
- GET `/api/setting/status` - ステータス取得（非推奨。/api/status を使用）
- GET `/api/setting/{key}` - 設定取得
- PUT `/api/setting/{key}` - 設定更新
- DELETE `/api/setting/{key}` - 設定削除
- GET `/api/setting/printer/{storeId}` - プリンター設定取得
- POST `/api/setting/printer/{storeId}` - プリンター設定保存
- GET `/api/setting/application` - アプリケーション設定取得
- POST `/api/setting/application` - アプリケーション設定保存

### Users API (/api/users)
- GET `/api/users` - ユーザー一覧取得
- GET `/api/users/{barcode}` - ユーザー取得

## 開発ガイドライン

API関連の作業（エンドポイントの追加・変更・削除）を行った際は、必ずOpenAPI仕様書（`/api.yaml`）を更新してください。

## クライアントSDKの自動生成

master ブランチの `api.yaml` が更新されると、GitHub Actions（`.github/workflows/generate-sdk.yml`）が Kotlin クライアントSDKを自動生成します（workflow_dispatch による手動実行も可能）。

- SDKバージョン: build.gradle の version + ワークフロー実行番号（例: 1.0.0.42）
- 生成方式: OpenAPI Generator（kotlin / jvm-okhttp4、パッケージ: info.nukoneko.kidspos.sdk）
- 配布: GitHub Release（タグ `sdk-vX.Y.Z.N`）に zip として添付
- クライアント連携: KidsPOSProject/KidsPOS-for-Android の `sdk/` ディレクトリを更新するPRを自動作成

クライアントPRの自動作成には GitHub App を使用します。以下のセットアップが必要です（未設定の場合、Release への添付までが実行されます）。

1. KidsPOSProject Organization で GitHub App を作成する
   - Repository permissions: Contents（Read and write）と Pull requests（Read and write）のみ
   - Webhook は不要（Active のチェックを外す）
2. 作成した App を KidsPOS-for-Android のみにインストールする
3. KidsPOS-Server の Secrets（Settings → Secrets and variables → Actions）に以下を登録する
   - `SDK_APP_ID`: App ID
   - `SDK_APP_PRIVATE_KEY`: App の秘密鍵（PEM ファイルの内容）

ワークフロー実行時に App の installation token（約1時間で自動失効）を発行して push と PR 作成に使用するため、長命の Personal Access Token は不要です。