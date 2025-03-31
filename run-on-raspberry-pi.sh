#!/bin/bash

# Raspberry Pi optimized startup script for KidsPOS Server

# Set memory limits for JVM
export JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication -Xlog:gc:file=gc.log"

# Run in background with optimized settings
java $JAVA_OPTS \
  -Dspring.profiles.active=prod \
  -Dserver.port=8080 \
  -Dlogging.level.root=WARN \
  -Dlogging.level.info.nukoneko.kidspos=INFO \
  -Dspring.main.lazy-initialization=true \
  -jar app.jar
