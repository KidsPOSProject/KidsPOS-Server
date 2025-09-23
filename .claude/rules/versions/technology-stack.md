# 技術スタックバージョン情報

## 最終更新: 2025年9月

## 現在のバージョン

### Core

- **Java**: 21 LTS
- **Kotlin**: 2.2.20
- **Spring Boot**: 3.2.0
- **Gradle**: 8.10

### Database

- **SQLite**: 3.44.1.0
- **Hibernate**: 6.x (Spring Boot 3.2.0に含まれる)
- **Flyway**: 9.22.3

### Testing

- **JUnit**: 5.10.1
- **MockK**: 1.13.8
- **AssertJ**: 3.24.2

### Code Quality

- **Detekt**: 1.23.4 (※Kotlin 2.2.20との互換性問題により一時的に無効化)
- **JaCoCo**: 0.8.11

### Documentation

- **SpringDoc OpenAPI**: 2.2.0

## バージョン選定の理由

### Gradle 8.10

- Kotlin 2.2.20との互換性
- Gradle 9.xはKotlin 2.2.20未対応のため8.10を使用

### Kotlin 2.2.20

- 2025年9月時点の最新安定版
- Multiplatform改善、Swift export対応

### Spring Boot 3.2.0

- Jakarta EE 9+対応
- Java 21サポート
- 長期サポート予定

## アップグレード計画

### 短期（1-3ヶ月）

- Detekt 2.0.0安定版へのアップグレード（Kotlin 2.2.20対応待ち）

### 中期（3-6ヶ月）

- Kotlin 2.3.xへのアップグレード（Gradle 9.x対応版）
- Gradle 9.xへのアップグレード

### 長期（6ヶ月以降）

- Spring Boot 3.3.xへのアップグレード（安定性確認後）

## 互換性マトリックス

| Component   | Min Version | Max Version | Notes           |
|-------------|-------------|-------------|-----------------|
| Java        | 21          | 25          | LTS推奨           |
| Kotlin      | 2.2.0       | 2.2.x       | Gradle 8.10必須   |
| Gradle      | 8.5         | 8.14        | Kotlin 2.2.20制限 |
| Spring Boot | 3.2.0       | 3.2.x       | Java 21必須       |
