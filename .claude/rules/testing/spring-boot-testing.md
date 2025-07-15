# Spring Boot テスト戦略

## 概要
Spring Boot アプリケーションの各レイヤーに対する効果的なテスト手法。

## テストの種類

### 単体テスト（Unit Test）
- 個々のクラスやメソッドのテスト
- 外部依存はモック化
- @Mock、@InjectMocks の使用
- 高速実行が可能

### 統合テスト（Integration Test）
- 複数のコンポーネント間の連携テスト
- @SpringBootTest の使用
- 実際のデータベースやサービスとの接続

### スライステスト
- @WebMvcTest: Controller レイヤーのテスト
- @DataJpaTest: Repository レイヤーのテスト
- @RestClientTest: REST クライアントのテスト

## Controller テスト

### @WebMvcTest の使用
```kotlin
@WebMvcTest(ItemController::class)
class ItemControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc
    
    @MockBean
    lateinit var itemService: ItemService
}
```

### MockMvc による検証
- HTTP ステータスコード
- レスポンスボディ
- ヘッダー情報
- コンテンツタイプ

## Service テスト

### モックの活用
- Repository のモック化
- 外部サービスのモック化
- ビジネスロジックの検証に集中

### トランザクションのテスト
- @Transactional の動作確認
- ロールバックの検証

## Repository テスト

### @DataJpaTest の使用
- インメモリデータベースでのテスト
- TestEntityManager の活用
- カスタムクエリメソッドの検証

### テストデータの準備
- @Sql でテストデータをロード
- TestEntityManager でプログラム的に作成

## テストデータ管理

### テストフィクスチャ
- 再利用可能なテストデータの定義
- Builder パターンの活用
- Factory メソッドの実装

### データベースの初期化
- @DirtiesContext でコンテキストリセット
- @Sql でのデータ投入と削除

## アサーション

### AssertJ の活用
- 流暢な API での検証
- カスタムアサーションの作成
- 例外のテスト

## テストの命名規則
- given_when_then パターン
- 日本語での説明的な名前も可
- テスト内容が明確にわかる名前