# Raspberry Pi Setup Guide for KidsPOS Server

## 必要要件

### ハードウェア要件
- Raspberry Pi 3B+ 以上（推奨: Raspberry Pi 4B）
- RAM: 2GB以上（推奨: 4GB以上）
- microSDカード: 16GB以上（推奨: 32GB）
- 安定した電源供給（5V/3A）

### ソフトウェア要件
- Raspberry Pi OS (64-bit推奨)
- Java 21 のランタイム（OpenJDK）

Raspberry Pi 上でソースをビルドすることはありません。GitHub Releases に添付される実行可能 jar（app.jar）を配置して動かすため、JDK も Git も Gradle も不要です。

## セットアップ手順

### 1. Raspberry Pi OSの準備

```bash
# システムを最新に更新
sudo apt update && sudo apt upgrade -y

# 必要なパッケージをインストール
sudo apt install -y curl
```

### 2. Java 21のインストール

```bash
# OpenJDK 21のランタイムをインストール
sudo apt install -y openjdk-21-jre-headless

# Javaバージョンを確認
java -version
```

### 3. 運用スクリプトの取得

scripts/raspberry-pi ディレクトリ一式を Raspberry Pi にコピーします。
インターネットに接続できる場合はリポジトリからダウンロードし、できない場合は別の PC でダウンロードして USB メモリや scp で持ち込みます。

```bash
curl -fL -o kidspos-scripts.tar.gz \
  https://github.com/KidsPOSProject/KidsPOS-Server/archive/refs/heads/main.tar.gz
tar xzf kidspos-scripts.tar.gz --strip-components=1 '*/scripts/raspberry-pi'
cd scripts/raspberry-pi
```

### 4. セットアップスクリプトの実行

install.sh がディレクトリ作成、運用スクリプトの配置、systemd ユニットの配置と自動起動の有効化、jar の導入、起動確認までを行います。

```bash
# GitHub Releases から最新の app.jar を導入（要インターネット接続）
sudo ./install.sh

# 持ち込んだ jar を導入（オフライン運用）
sudo ./install.sh /path/to/app.jar
```

配置先は /opt/kidspos で、jar は app.jar、DB は kidspos.db として作られます。
JVM のメモリ設定は systemd ユニット（scripts/raspberry-pi/kidspos-server.service）に -Xms256m -Xmx512m として含まれています。メモリの多い機種で増やす場合はユニットを編集してから daemon-reload と restart を行ってください。

実行後、そのまま doctor.sh による診断結果が表示されます。NG が出ていなければ起動は成功しています。

### 5. 環境変数の設定（必要な場合）

既定値のままで動作します。SSL やアクセス制限を変更したい場合のみ、systemd のドロップインで環境変数を渡します。

```bash
sudo systemctl edit kidspos-server
```

エディタが開いたら以下を記述します:

```ini
[Service]
Environment=SSL_ENABLED=false
Environment=ALLOWED_IP_PREFIX=192.168.
Environment=APK_UPLOAD_DIR=/opt/kidspos/uploads/apk
Environment=APK_MAX_FILE_SIZE=104857600
```

```bash
sudo systemctl daemon-reload
sudo systemctl restart kidspos-server
```

データベースファイルの場所は WorkingDirectory 直下の kidspos.db に固定されており、環境変数では変更できません。場所を変えたい場合は systemd ユニットの WorkingDirectory を変更してください。

### 6. 状態の確認

```bash
# 診断（Java、jar、サービス、ヘルスチェック、DB、ディスク、ログ、時刻）
/opt/kidspos/doctor.sh

# systemd のステータス
sudo systemctl status kidspos-server
```

### 7. ファイアウォール設定

```bash
# UFWをインストール（必要な場合）
sudo apt install -y ufw

# ポート8080を開放
sudo ufw allow 8080/tcp

# HTTPSを使用する場合
# sudo ufw allow 8443/tcp

# ファイアウォールを有効化
sudo ufw enable
```

### 8. パフォーマンス最適化

```bash
# Swapファイルのサイズを増やす（SDカードの寿命に注意）
sudo dphys-swapfile swapoff
sudo sed -i 's/CONF_SWAPSIZE=100/CONF_SWAPSIZE=1024/g' /etc/dphys-swapfile
sudo dphys-swapfile setup
sudo dphys-swapfile swapon

# CPU ガバナーを設定（パフォーマンスモード）
echo performance | sudo tee /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
```

## アプリケーションの更新

jar の差し替えは update-app.sh が行います。停止、DB と旧 jar のバックアップ、差し替え、起動、ヘルスチェックまで自動で、失敗した場合は旧バージョンに巻き戻します。

```bash
# ネット接続時: 最新リリースを取得して更新
sudo /opt/kidspos/update-app.sh

# オフライン時: 持ち込んだ jar で更新
sudo /opt/kidspos/update-app.sh /path/to/app.jar
```

DB スキーマの変更は起動時に Flyway が自動適用するため、追加の作業は不要です。
詳細は scripts/raspberry-pi/README.md を参照してください。

## トラブルシューティング

まず /opt/kidspos/doctor.sh を実行してください。原因の切り分けができます。

### メモリ不足エラー

systemd ユニットの ExecStart に含まれるヒープ設定を調整します。

```bash
sudo systemctl edit --full kidspos-server
# ExecStart の -Xms256m -Xmx512m を -Xms128m -Xmx384m などに変更

sudo systemctl daemon-reload
sudo systemctl restart kidspos-server
```

### ポートが使用中

```bash
# 使用中のポートを確認
sudo lsof -i :8080

# プロセスを終了
sudo kill -9 [PID]
```

### ログの確認

標準出力と標準エラーは journal に送られます。あわせてアプリ自身が logback で /opt/kidspos/logs/kidspos.log に書き出します。

```bash
# リアルタイムで追う
journalctl -u kidspos-server -f

# 最新100行を確認
journalctl -u kidspos-server -n 100

# エラーだけを抜き出す
journalctl -u kidspos-server -n 500 --no-pager | grep -E "ERROR|Exception"

# logback が書き出すファイル
tail -f /opt/kidspos/logs/kidspos.log
```

## セキュリティ推奨事項

1. **デフォルトパスワードの変更**
   - Raspberry Piのデフォルトユーザー（pi）のパスワードを変更
   - SSLキーストアのパスワードを変更

2. **定期的なアップデート**
   ```bash
   sudo apt update && sudo apt upgrade -y
   ```

3. **バックアップ**

   update-app.sh が更新のたびに DB と旧 jar を /opt/kidspos/backup/ に保存します（既定 5 世代）。
   任意のタイミングで取る場合は、書き込み中のコピーで壊れないよう必ずサービスを止めてから行います。

   ```bash
   sudo systemctl stop kidspos-server
   cp /opt/kidspos/kidspos.db /opt/kidspos/backup/kidspos-$(date +%Y%m%d).db
   sudo systemctl start kidspos-server
   ```

4. **アクセス制限**
   - ALLOWED_IP_PREFIX環境変数で内部ネットワークのみにアクセスを制限

## リソース監視

```bash
# CPU/メモリ使用状況
htop

# ディスク使用状況
df -h

# Java プロセスの確認
jps -v

# ネットワーク接続状況
netstat -tulpn | grep 8080
```

## 推奨される追加設定

### 1. 逆プロキシ（Nginx）の設定

```bash
# Nginxをインストール
sudo apt install -y nginx

# 設定ファイルを作成
sudo nano /etc/nginx/sites-available/kidspos
```

### 2. ログの保持量の調整

/opt/kidspos/logs/ のファイルは logback が日付ごとに 30 日分保持するため、logrotate の設定は不要です。
journal 側の消費を抑えたい場合は上限を設けます。

```bash
sudo mkdir -p /etc/systemd/journald.conf.d
sudo tee /etc/systemd/journald.conf.d/kidspos.conf << 'EOF'
[Journal]
SystemMaxUse=200M
MaxRetentionSec=1month
EOF

sudo systemctl restart systemd-journald
```

## よくある質問

### Q: Raspberry Pi Zero Wで動作しますか？
A: メモリが512MBと少ないため、動作は可能ですが推奨しません。最低でもRaspberry Pi 3B+を推奨します。

### Q: SDカードの寿命を延ばすには？
A:
- ログファイルを外部ストレージに保存
- Swapファイルの使用を最小限に
- 定期的なバックアップ

### Q: リモートアクセスを設定するには？
A: VPNの設定を推奨します。直接インターネットに公開する場合は、必ずHTTPSを有効にし、適切なファイアウォール設定を行ってください。