# KidsPOS Server

キッズビジネスタウンいちかわで使っている、子供向けPOSシステムのサーバーです。会場のレジ端末（[KidsPOS for Android](https://github.com/KidsPOSProject/KidsPOS-for-Android)）からの通信を受けて、商品・売上・スタッフ・店舗の情報をまとめて管理します。

Spring Boot と Kotlin で書かれていて、データベースは SQLite。サーバーと言っても大げさなものではなく、会場に持ち込んだノートPC一台で動きます。

## 動かすには

Java 21 が入っていれば準備は終わりです。

```bash
./gradlew bootRun
```

これで http://localhost:8080 に管理画面が立ち上がります。データベース（kidspos.db）は初回起動時に自動で作られるので、事前のセットアップは要りません。スキーマの変更は Flyway が起動時に反映します。

配布用の JAR がほしいときはこちら。

```bash
./gradlew bootJar
```

## 画面

運営中の管理はぜんぶブラウザからできます。

| URL | できること |
|-----|-----------|
| /items | 商品の登録、バーコードPDFの印刷 |
| /sales | 売上の確認 |
| /staffs | スタッフの登録 |
| /stores | 店舗の登録 |
| /settings | 設定 |

レジ端末とのやりとりは /api/ 以下の REST API で行います。仕様は api.yaml（OpenAPI）にまとまっているので、詳しくはそちらを見てください。Android 側のクライアントコードはこのファイルから自動生成されています。

## バーコードのこと

商品・スタッフ・売上にはそれぞれバーコードが振られます。形式は「A + 種別2桁 + ID6桁 + A」の10文字（例: A01000001A）。種別は 00 がスタッフ、01 が商品、02 が売上です。商品登録時にバーコードを指定しなければ、この形式で自動採番されます。

レシートはサーマルプリンターで印刷できます。プリンターの疎通状態は /api/status で確認できます。

## 開発

```bash
./gradlew build -x detekt   # ビルド
./gradlew test              # テスト
```

技術スタックは Spring Boot 3.2 + Kotlin 2.0、ビルドは Gradle 8.10。画面は Thymeleaf + Bootstrap + DataTables という素朴な構成です。API に変更を入れたときは api.yaml の更新を忘れずに。
