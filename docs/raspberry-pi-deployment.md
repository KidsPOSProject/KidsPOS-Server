# Raspberry Piへのデプロイガイド

このドキュメントでは、KidsPOSをRaspberry Piに実際にデプロイした手順を記録します。

## 検証済み環境

- **デバイス**: Raspberry Pi Zero W
- **OS**: Raspberry Pi OS (Debian Trixie)
- **ホスト名**: kidspos-server.local
- **IPアドレス**: 192.168.100.10
- **Java**: OpenJDK 21.0.8

## デプロイ手順

Raspberry Pi 上ではビルドしません。GitHub Releases に添付される実行可能 jar（app.jar）を配置して動かします。
セットアップと更新は scripts/raspberry-pi のスクリプトが行うため、手順は下記の 4 ステップです。

### 1. Java 21のインストール

```bash
ssh pi@kidspos-server.local

# パッケージリストを更新
sudo apt update && sudo apt upgrade -y

# OpenJDK 21のランタイムをインストール
sudo apt install -y openjdk-21-jre-headless

# インストール確認
java -version
# 出力例: openjdk version "21.0.8" 2025-07-15
```

### 2. 運用スクリプトの取得

インターネットに接続できる場合はリポジトリから取得します:

```bash
curl -fL -o kidspos-scripts.tar.gz \
  https://github.com/KidsPOSProject/KidsPOS-Server/archive/refs/heads/main.tar.gz
tar xzf kidspos-scripts.tar.gz --strip-components=1 '*/scripts/raspberry-pi'
cd scripts/raspberry-pi
```

接続できない場合はローカルマシンから転送します:

```bash
scp -r scripts/raspberry-pi pi@kidspos-server.local:~/kidspos-scripts
```

### 3. app.jarの用意

ネットに接続できる Raspberry Pi なら install.sh が GitHub Releases から自動取得するため、この手順は不要です。
オフライン運用の場合はローカルマシンでダウンロードして持ち込みます:

```bash
JAR_URL=$(curl -fsSL "https://api.github.com/repos/KidsPOSProject/KidsPOS-Server/releases?per_page=20" \
  | grep -o '"browser_download_url": *"[^"]*/app\.jar"' | head -1 | cut -d'"' -f4)
curl -fL -o app.jar "$JAR_URL"

scp app.jar pi@kidspos-server.local:~/app.jar
```

### 4. セットアップスクリプトの実行

ディレクトリ作成、運用スクリプトの配置、systemd ユニットの配置と自動起動の有効化、jar の導入、起動確認までを一度に行います。

```bash
# ネット接続時
sudo ./install.sh

# オフライン時
sudo ./install.sh ~/app.jar
```

配置される systemd ユニットの内容は scripts/raspberry-pi/kidspos-server.service です:

```ini
[Unit]
Description=KidsPOS Server
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=pi
WorkingDirectory=/opt/kidspos
ExecStart=/usr/bin/java -Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -jar /opt/kidspos/app.jar
AmbientCapabilities=CAP_SYS_TIME
CapabilityBoundingSet=CAP_SYS_TIME
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=kidspos-server

[Install]
WantedBy=multi-user.target
```

AmbientCapabilities=CAP_SYS_TIME はサーバーの時刻同期に必要です。イントラネットでは時刻同期サーバーに到達できず、Raspberry Pi は RTC を持たないため電源を入れるたびに時刻が巻き戻ります。この権限がないと同期は失敗し、画面に権限不足の旨が表示されます。

すでに稼働している Pi でこの権限が付いていない場合は update-app.sh を実行してください。update-app.sh は配布物の systemd ユニットと実機のユニットを毎回突き合わせ、差があれば入れ替えて daemon-reload と再起動まで行います。新しいユニットで起動できなかった場合は元のユニットに戻します。Pi 上の update-app.sh 自体が古い場合は、1 回目でスクリプトが新しくなり 2 回目でユニットが更新されるため、2 回実行してください。

### 時刻の合わせ方

管理画面の設定ページにある同期ボタンのほかに、クライアントが申告した時刻で自動的に合わせます。管理画面とレジアプリはすべてのリクエストに X-Client-Time ヘッダー（UNIX エポックミリ秒）を付けて送り、サーバーはずれが閾値を超えたときだけクールダウンを挟んで時刻を合わせます。管理画面を開くかレジが通信するだけで時刻が揃うため、電源投入後の手作業は不要です。

| 設定キー | 既定値 | 内容 |
| --- | --- | --- |
| app.system-time.auto-sync-enabled | true | 自動同期の有効・無効 |
| app.system-time.auto-sync-threshold-millis | 30000 | この値を超えてずれていたら同期する |
| app.system-time.auto-sync-cooldown-millis | 60000 | 同期を試みる間隔の下限 |

RTC が無いため、同期した時刻は fake-hwclock が保存した内容から次回起動時に復元されます。サーバーは時刻を変更した直後に fake-hwclock save を実行するので、同期後すぐ電源を落としても巻き戻りません。fake-hwclock が入っていない場合は sudo apt install -y fake-hwclock を実行してください（doctor.sh でも警告されます）。

標準出力と標準エラーは journal に送られます。あわせてアプリ自身が logback で /opt/kidspos/logs/kidspos.log に書き出し、日付ごとに 30 日分ローテーションします。

### 5. 起動確認

Raspberry Pi Zero Wは処理が遅いため、起動に約6-7分かかります。
install.sh は起動を待ってから doctor.sh の診断結果を表示するので、NG が出ていなければ起動は成功しています。

```bash
# ログをリアルタイムで確認
journalctl -u kidspos-server -f

# 起動完了メッセージを待つ
# "Started ServerApplicationKt in XXX seconds" が表示されれば完了

# 稼働診断
/opt/kidspos/doctor.sh
```

ブラウザで以下のURLにアクセス:
- http://192.168.100.10:8080
- http://kidspos-server.local:8080

## パフォーマンス情報

### 起動時間

| デバイス | 起動時間 | 備考 |
|---------|---------|------|
| Raspberry Pi Zero W | 約5-7分 | 初回起動時 |
| Raspberry Pi Zero W | 約5-6分 | 再起動時 |

### メモリ使用量

- 設定: `-Xms256m -Xmx512m`
- Raspberry Pi Zero Wの512MBメモリでも安定動作

### CPU使用率

起動時は高負荷（load average 4.0以上）ですが、起動完了後は安定します。

## 運用管理

### サービス管理コマンド

```bash
# サービスの開始
sudo systemctl start kidspos-server

# サービスの停止
sudo systemctl stop kidspos-server

# サービスの再起動
sudo systemctl restart kidspos-server

# ステータス確認
sudo systemctl status kidspos-server

# 自動起動の有効化
sudo systemctl enable kidspos-server

# 自動起動の無効化
sudo systemctl disable kidspos-server
```

### ログ確認

```bash
# アプリケーションログをリアルタイムで確認
journalctl -u kidspos-server -f

# 最新100行を確認
journalctl -u kidspos-server -n 100

# エラーだけを抜き出す
journalctl -u kidspos-server -n 500 --no-pager | grep -E "ERROR|Exception"

# logback が書き出すファイル（日付ごとに 30 日分保持）
tail -f /opt/kidspos/logs/kidspos.log
```

### アプリケーションの更新

update-app.sh が「停止 → DB と旧 jar のバックアップ → 差し替え → 起動 → ヘルスチェック」を行います。
ヘルスチェックが通らなかった場合は旧 jar と更新直前の DB に自動で巻き戻します。

```bash
# ネット接続時: 最新リリースを取得して更新（同一バージョンなら何もしない）
sudo /opt/kidspos/update-app.sh

# オフライン時: 持ち込んだ jar で更新
sudo /opt/kidspos/update-app.sh ~/app.jar
```

DB スキーマの変更は起動時に Flyway が自動適用するため、追加の作業は不要です。
Flyway は前進専用のため、DB を戻さずに jar だけ旧バージョンへ戻すことはしないでください。
バックアップは /opt/kidspos/backup/ に既定 5 世代保持されます。

詳細は scripts/raspberry-pi/README.md を参照してください。

## トラブルシューティング

まず doctor.sh を実行してください。Java、jar の形式、サービス、ヘルスチェック、DB、ディスク、ログ、時刻同期をまとめて確認し、OK / 注意 / NG と対処コマンドを表示します。

```bash
/opt/kidspos/doctor.sh
```

### サービスが起動しない場合

```bash
# サービスの詳細ステータスを確認
sudo systemctl status kidspos-server

# ログを確認
journalctl -u kidspos-server -n 50

# 前回起動時のログを確認
journalctl -u kidspos-server -b -1 --no-pager
```

### 起動が遅い場合

Raspberry Pi Zero Wは性能が限られているため、以下は正常です:
- 起動に5-7分かかる
- 起動時のCPU負荷が高い（load average 4.0以上）

より高速な起動が必要な場合は、Raspberry Pi 3以降の使用を推奨します。

### メモリ不足の場合

```bash
# ヒープサイズを調整（/etc/systemd/system/kidspos-server.service）
ExecStart=/usr/bin/java -Xms128m -Xmx384m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -jar /opt/kidspos/app.jar

# 変更後はリロードと再起動
sudo systemctl daemon-reload
sudo systemctl restart kidspos-server
```

### ポート8080が使用中の場合

別のポートを使用:

```bash
# /etc/systemd/system/kidspos-server.service の ExecStart に追加
ExecStart=/usr/bin/java -Dserver.port=8081 -Xms256m -Xmx512m -jar /opt/kidspos/app.jar

# 変更後
sudo systemctl daemon-reload
sudo systemctl restart kidspos-server
```

## 自動起動の検証

再起動して自動起動を確認:

```bash
# Raspberry Piを再起動
sudo reboot

# 再起動後、数分待ってから確認
ssh pi@kidspos-server.local "sudo systemctl status kidspos-server"

# アプリケーションにアクセス
curl http://kidspos-server.local:8080
```

## セキュリティ考慮事項

### ファイアウォール設定

```bash
# UFWをインストール（まだの場合）
sudo apt install ufw

# ポート8080を開放
sudo ufw allow 8080/tcp

# SSH接続を許可
sudo ufw allow ssh

# ファイアウォールを有効化
sudo ufw enable
```

### SSH鍵認証の設定

パスワード認証の代わりにSSH鍵認証を推奨:

```bash
# ローカルマシンから公開鍵をコピー
ssh-copy-id pi@kidspos-server.local
```

## ベンチマーク

### Raspberry Pi Zero W

- **初回起動時間**: 5分39秒
- **再起動後の起動時間**: 5分39秒
- **メモリ使用量**: 約300-400MB
- **アイドル時CPU使用率**: 低（起動完了後）

### 推奨環境

パフォーマンスが重要な場合:
- **Raspberry Pi 3 以降**を推奨
- **メモリ**: 1GB以上推奨
- **ストレージ**: 最小500MB、推奨1GB以上

## 関連リンク

- [運用スクリプトの説明](../scripts/raspberry-pi/README.md)
- [セットアップガイド](RASPBERRY_PI_SETUP.md)
- [GitHub Actions - Release Server](../.github/workflows/release-server.yml)
