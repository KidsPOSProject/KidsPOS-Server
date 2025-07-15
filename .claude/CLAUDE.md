# KidsPos プロジェクトガイドライン

## プロジェクト概要
KidsPosは、子供向けのPOSシステムで、Spring Boot + Kotlin + SQLiteで構築されています。

## 重要な実行指針

### 必須ルール
- **rules ディレクトリ内の全ルールを実行前に必ず読み込み、絶対に守ること**
- 各ルールファイルは汎用的で恒久的に利用可能な形式で記述されている

## アーキテクチャ

### 技術スタック
- **Backend**: Spring Boot 2.7.3 + Kotlin 1.6.21
- **Database**: SQLite + Hibernate JPA
- **Frontend**: Thymeleaf + Bootstrap + jQuery + DataTables
- **Build**: Gradle

### ディレクトリ構造
```
src/main/kotlin/info/nukoneko/kidspos/
├── common/          # 共通ユーティリティ
├── receipt/         # レシート印刷機能
└── server/
    ├── controller/  # コントローラー層
    │   ├── api/    # REST API
    │   └── front/  # Web UI
    ├── entity/      # エンティティ（DB モデル）
    ├── repository/  # リポジトリ層
    └── service/     # サービス層（ビジネスロジック）
```

## コーディング規約

### Kotlin
- Spring Boot の標準的な命名規則に従う
- エンティティは `@Entity` アノテーションを使用
- リポジトリは `JpaRepository` を継承
- サービスは `@Service` アノテーションを使用
- コントローラーは `@RestController` または `@Controller` を使用

### データベース
- SQLite を使用
- スキーマは `src/main/resources/tables.schema` で管理
- JPA エンティティと同期を保つ

## API 設計
- REST API は `/api/` プレフィックスを使用
- 標準的な HTTP メソッドとステータスコードを使用
- レスポンスは JSON 形式

## フロントエンド
- Thymeleaf テンプレートエンジンを使用
- Bootstrap 5.2.1 でスタイリング
- DataTables で表の高度な機能を実装

## ビルドとデプロイ
- `./gradlew bootJar` でビルド
- `./gradlew stage` で `app.jar` として配置
- ポート 8080 で起動

## 今後の改善点
- テストコードの実装が必要
- CI/CD パイプラインの構築が推奨
- API ドキュメントの自動生成（Swagger など）の導入検討