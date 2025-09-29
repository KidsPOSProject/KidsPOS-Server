# Raspberry Pi Setup Guide for KidsPOS Server

## 必要要件

### ハードウェア要件
- Raspberry Pi 3B+ 以上（推奨: Raspberry Pi 4B）
- RAM: 2GB以上（推奨: 4GB以上）
- microSDカード: 16GB以上（推奨: 32GB）
- 安定した電源供給（5V/3A）

### ソフトウェア要件
- Raspberry Pi OS (64-bit推奨)
- Java 21 (OpenJDK)
- Git

## セットアップ手順

### 1. Raspberry Pi OSの準備

```bash
# システムを最新に更新
sudo apt update && sudo apt upgrade -y

# 必要なパッケージをインストール
sudo apt install -y git curl wget unzip
```

### 2. Java 21のインストール

```bash
# OpenJDK 21をインストール（ARM64用）
sudo apt install -y openjdk-21-jdk

# Javaバージョンを確認
java -version
javac -version

# JAVA_HOMEを設定
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64' >> ~/.bashrc
echo 'export PATH=$PATH:$JAVA_HOME/bin' >> ~/.bashrc
source ~/.bashrc
```

### 3. プロジェクトのクローンとビルド

```bash
# プロジェクトをクローン
git clone https://github.com/KidsPOSProject/KidsPOS-Server.git
cd KidsPOS-Server

# ビルド（初回は時間がかかります）
./gradlew build -x detekt

# JARファイルを作成
./gradlew bootJar
```

### 4. メモリ最適化設定

Raspberry Piのメモリ制限に合わせてJVMパラメータを調整します。

```bash
# 起動スクリプトを作成
cat > run-kidspos.sh << 'EOF'
#!/bin/bash

# Raspberry Pi用のJVMメモリ設定
export JAVA_OPTS="-Xms256m -Xmx512m -XX:MaxMetaspaceSize=128m"

# Raspberry Pi 4B (4GB RAM)の場合
# export JAVA_OPTS="-Xms512m -Xmx1024m -XX:MaxMetaspaceSize=256m"

# アプリケーション起動
java $JAVA_OPTS -jar build/libs/kidspos-server-1.0.0.jar
EOF

chmod +x run-kidspos.sh
```

### 5. 環境変数の設定

```bash
# 環境変数ファイルを作成
cat > .env << 'EOF'
# SSL設定（HTTPSを使用する場合）
SSL_ENABLED=false
# SSL_KEY_STORE=/home/pi/kidspos/keystore/kidspos.p12
# SSL_KEY_STORE_PASSWORD=your_password

# APKアップロードディレクトリ
APK_UPLOAD_DIR=/home/pi/kidspos/uploads/apk
APK_MAX_FILE_SIZE=104857600

# データベースファイル
DB_PATH=/home/pi/kidspos/kidspos.db

# ネットワーク設定
ALLOWED_IP_PREFIX=192.168.
EOF

# 環境変数を読み込む
source .env
```

### 6. systemdサービスとして設定（自動起動）

```bash
# サービスファイルを作成
sudo cat > /etc/systemd/system/kidspos.service << 'EOF'
[Unit]
Description=KidsPOS Server
After=network.target

[Service]
Type=simple
User=pi
WorkingDirectory=/home/pi/KidsPOS-Server
EnvironmentFile=/home/pi/KidsPOS-Server/.env
ExecStart=/home/pi/KidsPOS-Server/run-kidspos.sh
Restart=on-failure
RestartSec=10

# メモリ制限
MemoryMax=1G
MemoryHigh=768M

[Install]
WantedBy=multi-user.target
EOF

# サービスを有効化
sudo systemctl daemon-reload
sudo systemctl enable kidspos.service
sudo systemctl start kidspos.service

# ステータスを確認
sudo systemctl status kidspos.service
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

## トラブルシューティング

### メモリ不足エラー

```bash
# JVMメモリ設定を調整
export JAVA_OPTS="-Xms128m -Xmx256m -XX:MaxMetaspaceSize=64m"
```

### ポートが使用中

```bash
# 使用中のポートを確認
sudo lsof -i :8080

# プロセスを終了
sudo kill -9 [PID]
```

### ログの確認

```bash
# systemdサービスのログ
journalctl -u kidspos.service -f

# アプリケーションログ
tail -f /home/pi/KidsPOS-Server/logs/kidspos.log
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
   ```bash
   # データベースのバックアップ
   cp /home/pi/kidspos/kidspos.db /home/pi/kidspos/backup/kidspos-$(date +%Y%m%d).db
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

### 2. ログローテーション

```bash
# logrotateの設定
sudo cat > /etc/logrotate.d/kidspos << 'EOF'
/home/pi/KidsPOS-Server/logs/*.log {
    daily
    rotate 7
    compress
    delaycompress
    missingok
    notifempty
    create 644 pi pi
}
EOF
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