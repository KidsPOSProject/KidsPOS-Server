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

### Settings API (/api/setting)
- GET `/api/setting` - 設定一覧取得
- POST `/api/setting` - 設定作成
- GET `/api/setting/status` - ステータス取得
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