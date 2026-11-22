#!/usr/bin/env sh
# Linux / macOS 发布构建入口。
# 依赖：JDK 27、Android SDK（compileSdk 37、build-tools 37.0.0）。
set -eu
cd "$(dirname "$0")"

JAVA_MAJOR=$(java -version 2>&1 | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | head -1)
if [ -z "${JAVA_MAJOR:-}" ] || [ "$JAVA_MAJOR" -lt 27 ]; then
  echo "需要 JDK 27 或更高版本，当前 java -version 不是 27+。" >&2
  exit 1
fi

ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$ANDROID_HOME" ] && [ -f local.properties ]; then
  ANDROID_HOME=$(sed -n 's/^sdk\.dir=//p' local.properties | tail -1)
fi
if [ -z "$ANDROID_HOME" ]; then
  echo "未找到 Android SDK：请设置 ANDROID_HOME/ANDROID_SDK_ROOT，或在 local.properties 写 sdk.dir。" >&2
  exit 1
fi
if [ ! -d "$ANDROID_HOME/platforms/android-37" ]; then
  echo "缺少 SDK Platform 37：$ANDROID_HOME/platforms/android-37" >&2
  exit 1
fi
if [ ! -d "$ANDROID_HOME/build-tools/37.0.0" ]; then
  echo "缺少 Build Tools 37.0.0：$ANDROID_HOME/build-tools/37.0.0" >&2
  exit 1
fi

if [ ! -f keystore.properties ]; then
  echo "缺少 keystore.properties：请复制 keystore.properties.example 并填入签名信息。" >&2
  exit 1
fi
for key in RXH_STORE_PASS RXH_KEY_PASS; do
  if ! grep -qE "^${key}=.+" keystore.properties; then
    echo "keystore.properties 缺少 ${key}=..." >&2
    exit 1
  fi
done
STORE_FILE=$(sed -n 's/^RXH_STORE_FILE=//p' keystore.properties | tail -1)
if [ -z "$STORE_FILE" ]; then STORE_FILE=keystore.jks; fi
case "$STORE_FILE" in
  /*) KS_PATH="$STORE_FILE" ;;
  *)  KS_PATH="$(pwd)/$STORE_FILE" ;;
esac
if [ ! -f "$KS_PATH" ]; then
  echo "找不到密钥库：$KS_PATH" >&2
  exit 1
fi

# ARM64 主机自动切到 SDK 自带的 aapt2，不再要求手工传 -P 参数。
ARCH=$(uname -m)
if [ "$ARCH" = "arm64" ] || [ "$ARCH" = "aarch64" ]; then
  AAPT2="$ANDROID_HOME/build-tools/37.0.0/aapt2"
  if [ ! -x "$AAPT2" ]; then
    echo "ARM64 主机需要可执行的 aapt2：$AAPT2" >&2
    exit 1
  fi
  set -- "$@" "-Pandroid.aapt2FromMavenOverride=$AAPT2"
fi

./gradlew assembleRelease "$@"
