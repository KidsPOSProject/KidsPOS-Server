# RESTful API 設計原則

## 概要
統一された RESTful API 設計により、予測可能で使いやすい API を提供する。

## URL 設計

### リソース指向
- 名詞を使用（動詞は避ける）
- 複数形を使用: `/items`、`/users`
- 階層構造を表現: `/stores/{storeId}/items`

### URL パターン
- コレクション: `/items`
- 個別リソース: `/items/{id}`
- 関連リソース: `/items/{id}/reviews`

## HTTP メソッドの使い分け

### GET
- リソースの取得
- 副作用なし（冪等性あり）
- キャッシュ可能

### POST
- 新規リソースの作成
- 副作用あり
- 成功時は 201 Created を返す

### PUT
- リソースの完全な更新
- 冪等性あり
- 存在しない場合は作成も可

### PATCH
- リソースの部分更新
- 冪等性あり
- 変更したいフィールドのみ送信

### DELETE
- リソースの削除
- 冪等性あり
- 成功時は 204 No Content を返す

## HTTP ステータスコード

### 成功レスポンス
- 200 OK: 一般的な成功
- 201 Created: リソース作成成功
- 204 No Content: 成功したが返すデータなし

### クライアントエラー
- 400 Bad Request: リクエストが不正
- 401 Unauthorized: 認証が必要
- 403 Forbidden: アクセス権限なし
- 404 Not Found: リソースが存在しない
- 409 Conflict: リソースの競合

### サーバーエラー
- 500 Internal Server Error: サーバー内部エラー
- 503 Service Unavailable: サービス利用不可

## レスポンス形式

### 成功時
```json
{
  "data": { ... },
  "message": "Success"
}
```

### エラー時
```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human readable message",
    "details": { ... }
  }
}
```

## ページネーション
- limit/offset または page/size パラメータを使用
- メタ情報をレスポンスに含める
- 総件数、現在のページ、総ページ数など