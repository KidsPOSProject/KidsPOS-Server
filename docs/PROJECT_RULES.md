# KidsPOS プロジェクトルール

## 開発ガイドライン

### 基本ルール
- Think in English, but generate responses in Japanese (思考は英語、回答の生成は日本語で行うように)

### API更新ルール
API関連の作業（エンドポイントの追加・変更・削除）を行った際は、必ずOpenAPI仕様書（`api.yaml`）を更新すること

### ビルド確認ルール
タスク完了時は必ずビルド（`./gradlew build -x detekt`）とアプリケーション起動（`./gradlew bootRun`）を確認すること

### 通貨単位
このプロジェクトでは通貨単位として「リバー」を使用
- 円（¥）ではなくリバーを使用
- 表示形式: `150リバー`

### バーコード形式
- バーコードはCODE39形式を使用
- QRコードではなくCODE39を使用

## プロジェクト構成ルール

### ドキュメント配置
- 説明文・ルール系のドキュメントは `docs/` に保存
- API仕様書は `/api.yaml` に配置
- プロジェクト固有の設定は `CLAUDE.md` に記載

### コーディングルール
- `docs/rules/` ディレクトリ内の全ルールを実行前に必ず読み込み、絶対に守ること
- 各ルールファイルは汎用的で恒久的に利用可能な形式で記述されている

## 技術スタック

- **Backend**: Spring Boot 3.2.0 + Kotlin 2.2.20
- **Database**: SQLite + Hibernate JPA
- **Build**: Gradle 8.10
- **Java**: OpenJDK 21

## ディレクトリ構造

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