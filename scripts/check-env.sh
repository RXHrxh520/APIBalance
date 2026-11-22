#!/usr/bin/env sh
# 构建环境自检。用法：scripts/check-env.sh [--release]
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
MODE="${1:-}"

fail() { echo "FAIL: $*" >&2; exit 1; }

command -v java >/dev/null 2>&1 || fail "找不到 java"
JAVA_MAJOR=$(java -version 2>&1 | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | head -1)
[ -n "${JAVA_MAJOR:-}" ] || fail "无法解析 java -version"
[ "$JAVA_MAJOR" -ge 27 ] || fail "需要 JDK 27+，当前 major=$JAVA_MAJOR"
echo "OK  JDK $JAVA_MAJOR"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ] && [ -f "$ROOT/local.properties" ]; then
  SDK=$(sed -n 's/^sdk\.dir=//p' "$ROOT/local.properties" | tail -1)
fi
[ -n "$SDK" ] || fail "未设置 ANDROID_HOME/ANDROID_SDK_ROOT，也没有 local.properties sdk.dir"
[ -d "$SDK/platforms/android-37" ] || fail "缺少 $SDK/platforms/android-37"
[ -d "$SDK/build-tools/37.0.0" ] || fail "缺少 $SDK/build-tools/37.0.0"
echo "OK  SDK $SDK"

AAPT2="$SDK/build-tools/37.0.0/aapt2"
[ -x "$AAPT2" ] && echo "OK  aapt2 $AAPT2" || fail "aapt2 不可执行：$AAPT2"

if [ "$MODE" = "--release" ]; then
  [ -f "$ROOT/keystore.properties" ] || fail "缺少 $ROOT/keystore.properties"
  for key in RXH_STORE_PASS RXH_KEY_PASS; do
    grep -qE "^${key}=.+" "$ROOT/keystore.properties" || fail "keystore.properties 缺少 $key"
  done
  STORE=$(sed -n 's/^RXH_STORE_FILE=//p' "$ROOT/keystore.properties" | tail -1)
  [ -n "$STORE" ] || STORE=keystore.jks
  case "$STORE" in
    /*) KS="$STORE" ;;
    *)  KS="$ROOT/$STORE" ;;
  esac
  [ -f "$KS" ] || fail "密钥库不存在：$KS"
  echo "OK  keystore $KS"
fi

echo "环境检查通过"
