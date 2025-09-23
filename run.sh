#!/bin/bash

# KidsPOS Server 起動スクリプト
# ポート8080を使用している既存プロセスを自動的に終了してから起動します

echo "🚀 KidsPOS Server 起動準備中..."

# ポート8080を使用しているプロセスをチェック
PORT_PID=$(lsof -ti:8080)

if [ ! -z "$PORT_PID" ]; then
    echo "⚠️  ポート8080が使用中です (PID: $PORT_PID)"
    echo "🔄 既存プロセスを終了しています..."
    kill -9 $PORT_PID 2>/dev/null
    sleep 2
    echo "✅ 既存プロセスを終了しました"
fi

# JAVA_HOMEの設定（環境変数が未設定の場合のみ）
if [ -z "$JAVA_HOME" ]; then
    # Java 21のデフォルトパスを探す
    if [ -d "$HOME/.sdkman/candidates/java/current" ]; then
        export JAVA_HOME="$HOME/.sdkman/candidates/java/current"
    elif [ -d "/usr/lib/jvm/java-21" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-21"
    elif command -v java >/dev/null 2>&1; then
        export JAVA_HOME=$(java -XshowSettings:properties -version 2>&1 | grep 'java.home' | cut -d'=' -f2 | tr -d ' ')
    else
        echo "❌ Java 21が見つかりません。JAVA_HOME環境変数を設定してください。"
        exit 1
    fi
fi

echo "📦 JAVA_HOME: $JAVA_HOME"

# アプリケーション起動
echo "🎯 KidsPOS Server を起動しています..."
echo "----------------------------------------"
./gradlew bootRun

# 起動失敗時のエラーハンドリング
if [ $? -ne 0 ]; then
    echo "❌ 起動に失敗しました"
    exit 1
fi