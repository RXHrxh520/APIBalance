#!/usr/bin/env sh
# Linux / macOS 发布构建入口。
# 依赖：JDK 17+（项目按 JDK 27 验证）、Android SDK（compileSdk 37、build-tools 37.0.0）。
set -e
cd "$(dirname "$0")"
if [ ! -f keystore.properties ]; then
  echo "缺少 keystore.properties：请复制 keystore.properties.example 并填入签名信息。" >&2
  exit 1
fi
./gradlew assembleRelease "$@"
