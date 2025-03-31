#!/bin/bash

# Raspberry Pi向けJVM最適化設定
export JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ParallelRefProcEnabled -XX:+UseStringDeduplication"

# アプリケーション起動
java $JAVA_OPTS -jar app.jar
