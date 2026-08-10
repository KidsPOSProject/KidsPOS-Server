# Raspberry Pi 運用スクリプト

Raspberry Pi 上で KidsPOS Server を運用するためのファイル一式です。
ハードウェア要件や OS・Java の導入手順は docs/RASPBERRY_PI_SETUP.md と docs/raspberry-pi-deployment.md を参照してください。

## 前提

- 配置先: /home/pi/kidspos（jar は kidspos.jar、DB は kidspos.db）
- サービス名: kidspos（systemd）
- サーバーは常時イントラネット内にあり、インターネットへはメンテナが明示的に接続したときのみ出られる運用を想定

## 初回セットアップ

```bash
mkdir -p /home/pi/kidspos
sudo cp kidspos.service /etc/systemd/system/kidspos.service
sudo systemctl daemon-reload
sudo systemctl enable kidspos.service

cp update-app.sh /home/pi/kidspos/
chmod +x /home/pi/kidspos/update-app.sh
```

初回の jar 配置も更新スクリプトで行えます（下記）。

## 更新方法

GitHub Releases の最新リリースに添付される app.jar を使って更新します。
DB スキーマの変更は jar 内の Flyway マイグレーションが起動時に自動適用されるため、追加の作業は不要です。

### ネット接続時（年1メンテナンスなど）

```bash
sudo /home/pi/kidspos/update-app.sh
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
sudo /home/pi/kidspos/update-app.sh /path/to/app.jar
```

## 失敗時の動作

起動後のヘルスチェック（/api/status）が通らない場合、スクリプトが自動で旧 jar と更新直前の DB バックアップに巻き戻して再起動します。
Flyway は前進専用のため、DB を戻さずに jar だけ旧バージョンへ戻すことはしないでください。

## バックアップ

- 保存先: /home/pi/kidspos/backup/
- 更新のたびに kidspos.db と旧 jar をタイムスタンプ付きで保存し、既定で 5 世代保持します
- 世代数は環境変数 KIDSPOS_BACKUP_KEEP で変更できます

## 設定の上書き

配置先やサービス名が異なる場合は環境変数で上書きできます:

| 環境変数 | 既定値 |
|---|---|
| KIDSPOS_APP_DIR | /home/pi/kidspos |
| KIDSPOS_JAR_NAME | kidspos.jar |
| KIDSPOS_SERVICE | kidspos |
| KIDSPOS_HEALTH_URL | http://localhost:8080/api/status |
| KIDSPOS_BACKUP_KEEP | 5 |
| KIDSPOS_REPO | KidsPOSProject/KidsPOS-Server |
