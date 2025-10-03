# Raspberry Piへのデプロイガイド

このドキュメントでは、KidsPOSをRaspberry Piに実際にデプロイした手順を記録します。

## 検証済み環境

- **デバイス**: Raspberry Pi Zero W
- **OS**: Raspberry Pi OS (Debian Trixie)
- **ホスト名**: kidspos-server.local
- **IPアドレス**: 192.168.100.10
- **Java**: OpenJDK 21.0.8

## デプロイ手順

### 1. Java 21のインストール

```bash
ssh pi@kidspos-server.local

# パッケージリストを更新
sudo apt update && sudo apt upgrade -y

# OpenJDK 21のインストール
sudo apt install -y openjdk-21-jdk

# インストール確認
java -version
# 出力例: openjdk version "21.0.8" 2025-07-15
```

### 2. アプリケーションディレクトリの作成

```bash
mkdir -p ~/kidspos
```

### 3. JARファイルの転送

ローカルマシンから実行:

```bash
# プロジェトルートでビルド
./gradlew build -x test -x detekt

# Raspberry Piに転送
scp build/libs/server-1.0.0.jar pi@kidspos-server.local:~/kidspos/kidspos.jar
```

### 4. Systemdサービスの設定

Raspberry Pi上で実行:

```bash
# サービスファイルを作成
sudo nano /etc/systemd/system/kidspos.service
```

以下の内容を入力:

```ini
[Unit]
Description=KidsPOS Server
After=network.target

[Service]
Type=simple
User=pi
WorkingDirectory=/home/pi/kidspos
ExecStart=/usr/bin/java -Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -jar /home/pi/kidspos/kidspos.jar
Restart=on-failure
RestartSec=10
StandardOutput=append:/home/pi/kidspos/kidspos.log
StandardError=append:/home/pi/kidspos/kidspos-error.log

[Install]
WantedBy=multi-user.target
```

### 5. サービスの有効化と起動

```bash
# Systemdをリロード
sudo systemctl daemon-reload

# サービスを有効化（自動起動）
sudo systemctl enable kidspos

# サービスを起動
sudo systemctl start kidspos

# ステータス確認
sudo systemctl status kidspos
```

### 6. 起動確認

Raspberry Pi Zero Wは処理が遅いため、起動に約6-7分かかります。

```bash
# ログをリアルタイムで確認
tail -f ~/kidspos/kidspos.log

# 起動完了メッセージを待つ
# "Started ServerApplicationKt in XXX seconds" が表示されれば完了
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
sudo systemctl start kidspos

# サービスの停止
sudo systemctl stop kidspos

# サービスの再起動
sudo systemctl restart kidspos

# ステータス確認
sudo systemctl status kidspos

# 自動起動の有効化
sudo systemctl enable kidspos

# 自動起動の無効化
sudo systemctl disable kidspos
```

### ログ確認

```bash
# アプリケーションログ
tail -f ~/kidspos/kidspos.log

# エラーログ
tail -f ~/kidspos/kidspos-error.log

# Systemdログ
journalctl -u kidspos -f

# 最新100行を確認
journalctl -u kidspos -n 100
```

### アプリケーションの更新

```bash
# 1. サービスを停止
sudo systemctl stop kidspos

# 2. 新しいJARファイルを転送（ローカルマシンから）
scp build/libs/server-1.0.0.jar pi@kidspos-server.local:~/kidspos/kidspos.jar

# 3. サービスを再起動
sudo systemctl start kidspos

# 4. ログで起動を確認
tail -f ~/kidspos/kidspos.log
```

## トラブルシューティング

### サービスが起動しない場合

```bash
# サービスの詳細ステータスを確認
sudo systemctl status kidspos

# ログを確認
journalctl -u kidspos -n 50

# エラーログを確認
cat ~/kidspos/kidspos-error.log
```

### 起動が遅い場合

Raspberry Pi Zero Wは性能が限られているため、以下は正常です:
- 起動に5-7分かかる
- 起動時のCPU負荷が高い（load average 4.0以上）

より高速な起動が必要な場合は、Raspberry Pi 3以降の使用を推奨します。

### メモリ不足の場合

```bash
# ヒープサイズを調整（/etc/systemd/system/kidspos.service）
ExecStart=/usr/bin/java -Xms128m -Xmx384m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -jar /home/pi/kidspos/kidspos.jar

# 変更後はリロードと再起動
sudo systemctl daemon-reload
sudo systemctl restart kidspos
```

### ポート8080が使用中の場合

別のポートを使用:

```bash
# /etc/systemd/system/kidspos.service の ExecStart に追加
ExecStart=/usr/bin/java -Dserver.port=8081 -Xms256m -Xmx512m -jar /home/pi/kidspos/kidspos.jar

# 変更後
sudo systemctl daemon-reload
sudo systemctl restart kidspos
```

## 自動起動の検証

再起動して自動起動を確認:

```bash
# Raspberry Piを再起動
sudo reboot

# 再起動後、数分待ってから確認
ssh pi@kidspos-server.local "sudo systemctl status kidspos"

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

- [README.md - Raspberry Piへのデプロイ](../README.md#raspberry-piへのデプロイ)
- [GitHub Actions - Release Build](.github/workflows/release-build.yml)
