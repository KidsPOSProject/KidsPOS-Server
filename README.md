# KidsPOS Server

キッズビジネスタウンいちかわで使用する子供向けPOSシステムのサーバーアプリケーションです。レジ端末（[KidsPOS for Android](https://github.com/KidsPOSProject/KidsPOS-for-Android)）と連携し、商品・売上・スタッフ・店舗の情報を管理します。

## 機能

- 商品管理（登録、バーコードPDF出力）
- 売上管理
- スタッフ管理
- 店舗管理
- レシート印刷（サーマルプリンター対応）
- レジ端末連携用 REST API

## 技術スタック

- Spring Boot 3.2 / Kotlin 2.0 / Java 21
- SQLite + Hibernate JPA + Flyway
- Thymeleaf + Bootstrap + DataTables
- Gradle 8.10

## 必要環境

- Java 21 以上

## 起動

```bash
./gradlew bootRun
```

http://localhost:8080 で管理画面にアクセスできます。データベース（kidspos.db）は初回起動時に自動生成されます。

実行可能 JAR の作成:

```bash
./gradlew bootJar
```

## Raspberry Pi へのデプロイ

Raspberry Pi 上ではビルドしません。GitHub Releases に添付される app.jar を配置して動かすため、必要なのは Java 21 のランタイムだけです。

```bash
sudo ./scripts/raspberry-pi/install.sh   # 初回セットアップと jar の導入
sudo /home/pi/kidspos/update-app.sh      # 更新（失敗時は自動で巻き戻し）
/home/pi/kidspos/doctor.sh               # 稼働診断
```

手順の詳細は [scripts/raspberry-pi/README.md](scripts/raspberry-pi/README.md)、機種ごとの実績は [docs/raspberry-pi-deployment.md](docs/raspberry-pi-deployment.md) を参照してください。

## 画面

| URL | 内容 |
|-----|------|
| /items | 商品の登録、バーコードPDFの出力 |
| /sales | 売上の一覧 |
| /staffs | スタッフの管理 |
| /stores | 店舗の管理 |
| /settings | 設定 |

## API

レジ端末との通信は /api/ 以下の REST API で行います。仕様は [api.yaml](api.yaml)（OpenAPI）を参照してください。Android 側のクライアントコードはこのファイルから自動生成されます。

バーコード形式は A + 種別2桁 + ID6桁 + A の10文字です（例: A01000001A、00: スタッフ / 01: 商品 / 02: 売上）。商品登録時に未指定の場合は自動採番されます。

## 開発

```bash
./gradlew build -x detekt   # ビルド
./gradlew test              # テスト
```

API を変更した場合は api.yaml を更新してください。
