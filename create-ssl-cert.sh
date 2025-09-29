#!/bin/bash

# 自己署名SSL証明書を作成するスクリプト

KEYSTORE_DIR="src/main/resources/keystore"
KEYSTORE_FILE="$KEYSTORE_DIR/kidspos.p12"
ALIAS="kidspos"
VALIDITY=3650
STOREPASS="kidspos123"
DNAME="CN=KidsPOS Server, OU=Development, O=KidsPOS, L=Tokyo, ST=Tokyo, C=JP"

echo "====================================="
echo " KidsPOS 自己署名証明書生成スクリプト"
echo "====================================="
echo ""

# キーストアディレクトリを作成
if [ ! -d "$KEYSTORE_DIR" ]; then
    echo "キーストアディレクトリを作成中..."
    mkdir -p "$KEYSTORE_DIR"
fi

# 既存のキーストアをバックアップ
if [ -f "$KEYSTORE_FILE" ]; then
    echo "既存のキーストアをバックアップ中..."
    mv "$KEYSTORE_FILE" "$KEYSTORE_FILE.backup.$(date +%Y%m%d%H%M%S)"
fi

# 自己署名証明書を生成
echo "自己署名証明書を生成中..."
keytool -genkeypair \
    -alias "$ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity "$VALIDITY" \
    -dname "$DNAME" \
    -keypass "$STOREPASS" \
    -keystore "$KEYSTORE_FILE" \
    -storepass "$STOREPASS" \
    -storetype PKCS12 \
    -ext "SAN=DNS:localhost,DNS:kidspos.local,IP:127.0.0.1,IP:192.168.1.100"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ 証明書の生成が完了しました！"
    echo ""
    echo "生成された証明書情報:"
    echo "  ファイル: $KEYSTORE_FILE"
    echo "  エイリアス: $ALIAS"
    echo "  パスワード: $STOREPASS"
    echo "  有効期限: $VALIDITY 日"
    echo ""
    echo "HTTPSを有効にするには、以下の環境変数を設定してください:"
    echo "  export SSL_ENABLED=true"
    echo ""
    echo "または、起動時に指定:"
    echo "  SSL_ENABLED=true ./gradlew bootRun"
    echo ""
    echo "証明書の詳細を確認:"
    keytool -list -v -keystore "$KEYSTORE_FILE" -storepass "$STOREPASS" | head -20
else
    echo ""
    echo "❌ 証明書の生成に失敗しました"
    exit 1
fi