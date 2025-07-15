# Spring アノテーションの使用規則

## 概要
Spring Framework のアノテーションを適切に使用し、コードの可読性と保守性を高める。

## コンポーネントアノテーション

### @Controller / @RestController
- Web レイヤーのコンポーネントに使用
- @Controller: View を返す場合
- @RestController: JSON/XML などのデータを返す場合

### @Service
- ビジネスロジックを含むサービスクラスに使用
- トランザクション管理が必要な場合は @Transactional と併用

### @Repository
- データアクセスレイヤーのコンポーネントに使用
- Spring Data JPA を使用する場合は自動的に例外変換が行われる

### @Component
- 上記に該当しない汎用的なコンポーネントに使用
- ユーティリティクラスなど

## マッピングアノテーション

### HTTP メソッドマッピング
- @GetMapping: 取得操作
- @PostMapping: 作成操作
- @PutMapping: 更新操作（全体）
- @PatchMapping: 更新操作（部分）
- @DeleteMapping: 削除操作

### パスパラメータとリクエストパラメータ
- @PathVariable: URL パスの一部を変数として受け取る
- @RequestParam: クエリパラメータを受け取る
- @RequestBody: リクエストボディを受け取る

## トランザクション管理
- @Transactional: メソッドまたはクラスレベルで使用
- 読み取り専用の場合: @Transactional(readOnly = true)
- 例外時のロールバック設定を明示的に指定