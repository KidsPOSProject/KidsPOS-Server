# Raspberry Pi 運用スクリプト

Raspberry Pi 上で KidsPOS Server を運用するためのファイル一式です。
ハードウェア要件や OS・Java の導入手順は docs/RASPBERRY_PI_SETUP.md と docs/raspberry-pi-deployment.md を参照してください。

## 前提

- 配置先: /opt/kidspos（jar は app.jar、DB は kidspos.db）
- サービス名: kidspos-server（systemd）
- サーバーは常時イントラネット内にあり、インターネットへはメンテナが明示的に接続したときのみ出られる運用を想定
- Raspberry Pi 上ではビルドしません。GitHub Releases に添付される app.jar を差し替えるだけなので、必要なのは Java ランタイムのみです

## ファイル

| ファイル | 用途 |
|---|---|
| install.sh | 初回セットアップ（ディレクトリ作成、systemd ユニット配置、自動起動有効化、jar 導入） |
| update-app.sh | jar の更新（バックアップ、差し替え、ヘルスチェック、失敗時の巻き戻し） |
| doctor.sh | 稼働診断（Java、jar、サービス、ヘルスチェック、DB、ディスク、ログ、時刻） |
| kidspos-server.service | systemd ユニットのテンプレート |
| test-install.sh / test-update-app.sh / test-doctor.sh | 上記スクリプトの自動テスト |

## 初回セットアップ

このディレクトリを Raspberry Pi にコピーし、install.sh を実行します。
必要なコマンドの確認、ディレクトリ作成、update-app.sh と doctor.sh の配置、systemd ユニットの配置と自動起動の有効化、jar の導入、最後に診断まで一度に行います。

```bash
sudo ./install.sh                  # GitHub Releases から最新の app.jar を導入（要インターネット接続）
sudo ./install.sh /path/to/app.jar # 持ち込んだ jar を導入（オフライン運用）
sudo ./install.sh --no-jar         # セットアップのみ行い jar は導入しない
```

jar が既に配置済みの場合は入れ替えず、サービスの起動のみ行います。
同じ引数で再実行しても設定は壊れません（systemd ユニットは内容が変わったときだけ書き換えます）。

サービスを起動した場合は、最後の診断に進む前に /api/status が応答するまで待ちます。
Raspberry Pi では起動完了まで数分かかり、待たずに診断すると必ず応答なしと判定されるためです。
既定は 2 秒間隔で最大 10 分待ち、応答を確認できないまま上限に達した場合も失敗にはせず、そのまま診断へ進みます。

Java が未導入、または Java 21 未満の場合は導入コマンドを表示して停止します。

```bash
sudo apt install -y openjdk-21-jre-headless
```

## 更新方法

GitHub Releases の最新リリースに添付される app.jar を使って更新します。
DB スキーマの変更は jar 内の Flyway マイグレーションが起動時に自動適用されるため、追加の作業は不要です。

### ネット接続時（年1メンテナンスなど）

```bash
sudo /opt/kidspos/update-app.sh
```

最新リリースを確認し、未適用なら「停止 → DB と旧 jar のバックアップ → 差し替え → 起動 → ヘルスチェック」を自動で行います。
同一バージョンなら何もしません（--force で強制再インストール）。

### オフライン時（jar を持ち込む場合）

別の PC で最新リリースの app.jar をダウンロードしておきます:

```bash
curl -fL -o app.jar https://github.com/KidsPOSProject/KidsPOS-Server/releases/latest/download/app.jar
```

USB メモリやイントラネット経由（scp など）で Raspberry Pi にコピーし、パスを渡して実行します:

```bash
sudo /opt/kidspos/update-app.sh /path/to/app.jar
```

## 状況の診断

不調のときや更新の前後に実行すると、どこが原因かを OK / 注意 / NG で切り分けられます。
NG が 1 件でもあれば終了コードは 1 になるため、cron や監視から呼び出すこともできます。

```bash
/opt/kidspos/doctor.sh
```

診断する項目:

- Java のバージョン（21 以上か）
- jar の有無と形式（zip として壊れていないか、plain jar が誤って置かれていないか）
- 導入バージョンと、GitHub Releases の最新版との差（オフライン時はスキップ）
- サービスの自動起動設定、起動状態、再起動の繰り返し
- 同じ jar を起動する別名の systemd ユニットが残っていないか（1 つの DB を 2 プロセスが掴むと破損します）
- ヘルスチェック（/api/status）とポートの待ち受け
- DB の有無、ディレクトリ所有者とサービス実行ユーザーの一致
- APK のアップロード領域（uploads）の有無
- ディスクの空き容量と DB バックアップの世代数
- journal の直近 500 行のエラー件数
- 時刻同期（売上の記録時刻がずれないか）

## 失敗時の動作

起動後のヘルスチェック（/api/status）が通らない場合、スクリプトが自動で旧 jar と更新直前の DB バックアップに巻き戻して再起動します。
Raspberry Pi Zero W などの低速な機種では起動に数分かかることがあるため、ヘルスチェックは 2 秒間隔で最大 20 分待ちます（起動を検知した時点で即終了します）。待ち時間は環境変数 KIDSPOS_HEALTH_RETRIES（回数）で、1 回あたりの応答待ちは KIDSPOS_HEALTH_TIMEOUT（秒、既定 10）で調整できます。
応答しないまま接続が保たれた場合でもタイムアウトで打ち切るため、ヘルスチェックが止まったままにはなりません。
Flyway は前進専用のため、DB を戻さずに jar だけ旧バージョンへ戻すことはしないでください。

## バックアップ

- 保存先: /opt/kidspos/backup/
- 更新のたびに kidspos.db と旧 jar をタイムスタンプ付きで保存し、既定で 5 世代保持します
- 世代数は環境変数 KIDSPOS_BACKUP_KEEP で変更できます

## 設定の上書き

配置先やサービス名が異なる場合は環境変数で上書きできます:

| 環境変数 | 既定値 | 対象 |
|---|---|---|
| KIDSPOS_APP_DIR | /opt/kidspos | install / update / doctor |
| KIDSPOS_JAR_NAME | app.jar | install / update / doctor |
| KIDSPOS_SERVICE | kidspos-server | install / update / doctor |
| KIDSPOS_HEALTH_URL | http://localhost:8080/api/status | install / update / doctor |
| KIDSPOS_REPO | KidsPOSProject/KidsPOS-Server | update / doctor |
| KIDSPOS_REQUIRED_JAVA_MAJOR | 21 | install / doctor |
| KIDSPOS_HEALTH_RETRIES | update は 600、install は 300 | install / update |
| KIDSPOS_HEALTH_TIMEOUT | 10 | install / update |
| KIDSPOS_BACKUP_KEEP | 5 | update |
| KIDSPOS_SERVICE_USER | pi | install |
| KIDSPOS_UNIT_DIR | /etc/systemd/system | install / doctor |
| KIDSPOS_LOG_LINES | 500 | doctor |
| KIDSPOS_DISK_WARN_MB | 500 | doctor |
| KIDSPOS_DISK_NG_MB | 100 | doctor |

## テスト

スクリプトの挙動は bash のテストで検証しています。systemctl・curl・java などは実行せずスタブに差し替えるため、開発機でそのまま実行できます。

```bash
bash scripts/raspberry-pi/test-install.sh
bash scripts/raspberry-pi/test-update-app.sh
bash scripts/raspberry-pi/test-doctor.sh
```
