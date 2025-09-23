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

# JAVA_HOMEの設定
export JAVA_HOME=/Users/atsumi/.local/share/mise/installs/java/openjdk-21.0.2
echo "📦 JAVA_HOME を設定: $JAVA_HOME"

# アプリケーション起動
echo "🎯 KidsPOS Server を起動しています..."
echo "----------------------------------------"
JAVA_HOME=$JAVA_HOME ./gradlew bootRun

# 起動失敗時のエラーハンドリング
if [ $? -ne 0 ]; then
    echo "❌ 起動に失敗しました"
    exit 1
fi