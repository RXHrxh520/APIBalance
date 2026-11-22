#!/usr/bin/env sh
# 调试构建入口：不需要 release keystore。
set -eu
cd "$(dirname "$0")/.."
"$(dirname "$0")/check-env.sh"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ] && [ -f local.properties ]; then
  SDK=$(sed -n 's/^sdk\.dir=//p' local.properties | tail -1)
fi
ARCH=$(uname -m)
if [ "$ARCH" = "arm64" ] || [ "$ARCH" = "aarch64" ]; then
  AAPT2="$SDK/build-tools/37.0.0/aapt2"
  [ -x "$AAPT2" ] || { echo "ARM64 缺少 aapt2：$AAPT2" >&2; exit 1; }
  set -- "$@" "-Pandroid.aapt2FromMavenOverride=$AAPT2"
fi
./gradlew assembleDebug "$@"
