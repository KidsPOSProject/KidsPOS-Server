# JPA ベストプラクティス

## 概要

Spring Data JPA を使用する際の推奨事項とパフォーマンス最適化の指針。

## エンティティ設計

### 基本原則

- @Entity アノテーションを使用
- @Id で主キーを指定
- @GeneratedValue で自動生成戦略を設定
- 適切な @Column 設定（nullable、unique、length など）

### 命名規則

- テーブル名: スネークケース（@Table(name = "table_name")）
- カラム名: スネークケース（@Column(name = "column_name")）
- エンティティクラス: パスカルケース
- フィールド: キャメルケース

## リレーションシップ

### @OneToMany / @ManyToOne

- 双方向の場合は mappedBy を使用
- FetchType.LAZY をデフォルトとする
- カスケードタイプは慎重に選択

### @ManyToMany

- 中間テーブルを明示的に定義することを推奨
- @JoinTable で中間テーブルをカスタマイズ

## クエリ最適化

### N+1 問題の回避

- @EntityGraph を使用した fetch join
- JPQL での JOIN FETCH
- 適切な FetchType の選択

### クエリメソッド

- Spring Data JPA のメソッド命名規則を活用
- 複雑なクエリは @Query アノテーションを使用
- ネイティブクエリは最終手段

## トランザクション管理

### @Transactional の適切な使用

- Service レイヤーで使用
- readOnly = true で読み取り専用を明示
- 適切な isolation レベルの設定

### 遅延読み込みの管理

- Open Session in View パターンの理解
- DTO への変換タイミングの考慮

## パフォーマンス最適化

### バッチ処理

- spring.jpa.properties.hibernate.jdbc.batch_size の設定
- saveAll() メソッドの活用

### キャッシュ

- 一次キャッシュ（EntityManager）の理解
- 二次キャッシュの適切な設定

### インデックス

- 頻繁に検索されるカラムにインデックスを設定
- 複合インデックスの順序に注意