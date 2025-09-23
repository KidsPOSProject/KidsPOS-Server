# KidsPOS (キッズPOS)

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-purple)
![Java](https://img.shields.io/badge/Java-21-orange)
![Gradle](https://img.shields.io/badge/Gradle-8.10-blue)
![License](https://img.shields.io/badge/license-MIT-blue)
![Coverage](https://img.shields.io/badge/coverage-85%25-brightgreen)
![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0-brightgreen)

子供向け教育用POSシステム - 楽しみながら商業活動を体験できる教育ツール

## 概要

KidsPOSは、子供たちが楽しみながら商業活動を体験できる教育用POSシステムです。学校のイベントや教育プログラムで、実際の小売業務を安全に体験できる環境を提供します。

### 主要機能

- **売上管理** - 商品の販売と売上記録を管理
- **在庫管理** - 商品の登録、バーコード管理、価格設定
- **スタッフ管理** - スタッフの登録とバーコードIDシステム
- **店舗管理** - 複数店舗の設定と管理
- **レシート印刷** - サーマルプリンター対応のレシート発行

### 使用シナリオ

- 学校の文化祭や学園祭での模擬店運営
- 子供向けイベントでの体験型学習
- 教育プログラムでの実践的な商業教育
- サマーキャンプでのトークンエコノミー
- 家族でのお店屋さんごっこ

## 技術スタック

### バックエンド

- Spring Boot 3.2.0
- Kotlin 2.2.20
- Java 21
- Gradle 8.10
- SQLite (組み込みデータベース)
- Hibernate JPA + Flyway (データベースマイグレーション)

### フロントエンド

- Thymeleaf (テンプレートエンジン)
- Bootstrap 5.2.1 (UIフレームワーク)
- jQuery 3.6.0
- DataTables 1.10.18 (高度なテーブル機能)

### システム要件

- Java 21以上
- メモリ: 最小512MB（推奨1GB）
- ポート: 8080（デフォルト）

## インストールとセットアップ

### 1. リポジトリのクローン

```bash
git clone https://github.com/KidsPOSProject/KidsPOS-Server.git
cd KidsPOS-Server
```

### 2. ビルド

```bash
# アプリケーションのビルド
./gradlew build

# 実行可能JARファイルの作成
./gradlew bootJar
```

### 3. 起動

開発環境での起動:

```bash
./gradlew bootRun
```

本番環境での起動:

```bash
# JARファイルを作成してステージング
./gradlew stage

# アプリケーションを起動
java -jar app.jar
```

### データベース設定

SQLiteデータベース（`kidspos.db`）は初回起動時に自動生成されます。追加の設定は不要です。

## 使用方法

### アクセス

ブラウザで以下のURLにアクセスしてください:

```
http://localhost:8080
```

### 主要画面

| 機能     | URL         | 説明          |
|--------|-------------|-------------|
| ホーム    | `/`         | ダッシュボード画面   |
| 商品管理   | `/items`    | 商品の登録・編集・削除 |
| 売上管理   | `/sales`    | 売上履歴の確認と管理  |
| スタッフ管理 | `/staffs`   | スタッフの登録と管理  |
| 店舗管理   | `/stores`   | 店舗情報の設定     |
| 設定     | `/settings` | システム設定の管理   |

### API

REST APIは `/api/` プレフィックスで利用可能です:

- `/api/item` - 商品API
- `/api/sale` - 売上API
- `/api/staff` - スタッフAPI
- `/api/store` - 店舗API
- `/api/setting` - 設定API

#### APIドキュメント

Swagger UIが利用可能です:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI仕様: http://localhost:8080/v3/api-docs

### 基本操作フロー

1. **商品を登録** - 商品管理画面から商品を追加
2. **スタッフを登録** - スタッフ管理画面でスタッフを追加
3. **店舗を設定** - 店舗管理画面で店舗情報を設定
4. **売上処理を実行** - POSレジ画面から商品をスキャンして販売

## 開発者向け情報

### プロジェクト構造

```
src/main/kotlin/info/nukoneko/kidspos/
├── common/          # 共通ユーティリティ
│   ├── CharExtensions.kt
│   ├── Commander.kt
│   ├── IntExtensions.kt
│   ├── PrintCommand.kt
│   └── StringExtensions.kt
├── receipt/         # レシート印刷機能
│   ├── ReceiptDetail.kt
│   └── ReceiptPrinter.kt
└── server/
    ├── controller/  # コントローラー層
    │   ├── api/    # REST APIコントローラー
    │   └── front/  # Web UIコントローラー
    ├── entity/      # JPA エンティティ
    ├── repository/  # データアクセス層
    └── service/     # ビジネスロジック層
```

### 開発コマンド

```bash
# テスト実行
./gradlew test

# コードカバレッジレポート生成
./gradlew jacocoTestReport

# 静的コード分析
./gradlew detekt

# アプリケーションのビルド
./gradlew build

# デプロイ用JARファイルの準備
./gradlew stage

# ビルドのクリーンアップ
./gradlew clean

# ステージングJARのクリーンアップ
./gradlew cleanJar
```

### コントリビューション

#### 貢献方法

1. **Issueを作成** - バグ報告や機能要望を[Issues](https://github.com/KidsPOSProject/KidsPOS-Server/issues)に投稿
2. **フォーク＆ブランチ作成** - リポジトリをフォークし、機能ブランチを作成
3. **変更をコミット** - 明確なコミットメッセージで変更を記録
4. **プルリクエストを送信** - masterブランチへのPRを作成

#### コーディング規約

- Kotlin公式コーディング規約に準拠
- Spring Bootのベストプラクティスを遵守
- 明確で意味のある変数名・関数名を使用
- KDocによる包括的なドキュメンテーション
- detektによる静的コード分析の実施

## ライセンス

MITライセンス - 詳細は[LICENSE](LICENSE)ファイルを参照してください。

## サポート

### 問題報告・要望

- [GitHub Issues](https://github.com/KidsPOSProject/KidsPOS-Server/issues)で問題を報告してください

### 連絡先

プロジェクトメンテナーへの連絡は、GitHubのIssueを通じてお願いします。

## 関連リソース

- [プロジェクトWiki](https://github.com/KidsPOSProject/KidsPOS-Server/wiki)（準備中）
- [APIドキュメント](docs/api.md)（準備中）
- [デプロイメントガイド](docs/deployment.md)（準備中）

## 品質保証

### テスト

- JUnit 5によるユニットテスト・統合テスト
- MockKによるモック作成
- JaCoCo統合によるコードカバレッジ測定
- 現在のコードカバレッジ: 85%以上
- セキュリティテスト (OWASP準拠)
- アーキテクチャテスト

### 静的コード分析

- detektによるKotlinコード品質チェック
- カスタムルールセット適用（`config/detekt/detekt.yml`）

### APIドキュメント

- OpenAPI 3.0仕様準拠
- Swagger UI統合による対話的APIテスト環境

## 最近の改善

### アーキテクチャ改善

- サービス層の責務分離とクリーンアーキテクチャの適用
- DTOパターンの導入によるレイヤー間の疎結合化
- 例外処理の統一化（GlobalExceptionHandler）
- データベースマイグレーション（Flyway）の導入

### 品質向上

- テストカバレッジ: 3% → 85%以上に向上
- 40以上のテストファイル追加
- OWASP準拠のセキュリティテスト実装
- Detektによる静的コード分析の強化

### 開発効率化

- Kiro仕様駆動開発フレームワークの導入
- Version Catalog (libs.versions.toml) によるバージョン管理
- OpenAPI/Swagger統合
- キャッシュ最適化

## 今後の改善予定

- CI/CDパイプラインの完全自動化
- 多言語対応（i18n）
- クラウドネイティブ対応
- マイクロサービス化の検討
- リアルタイムデータ同期機能

---

<div align="center">

**KidsPOS** - 子供たちに楽しい学びの体験を

</div>
