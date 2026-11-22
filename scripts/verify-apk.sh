#!/usr/bin/env sh
# 校验 APK：签名证书、zipalign、版本号、SHA-256。
set -eu
APK="${1:-app/build/outputs/apk/release/app-release.apk}"
[ -f "$APK" ] || { echo "找不到 APK：$APK" >&2; exit 1; }

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ] && [ -f local.properties ]; then
  SDK=$(sed -n 's/^sdk\.dir=//p' local.properties | tail -1)
fi
BT="$SDK/build-tools/37.0.0"
[ -d "$BT" ] || { echo "找不到 build-tools：$BT" >&2; exit 1; }

APKSIGNER="$BT/apksigner"
[ -x "$APKSIGNER" ] || { echo "找不到 apksigner：$APKSIGNER" >&2; exit 1; }
"$APKSIGNER" verify --print-certs "$APK"

ZIPALIGN="$BT/zipalign"
[ -x "$ZIPALIGN" ] || { echo "找不到 zipalign：$ZIPALIGN" >&2; exit 1; }
"$ZIPALIGN" -c -v 4 "$APK" >/dev/null
echo "zipalign: OK"

AAPT2="$BT/aapt2"
if [ -x "$AAPT2" ]; then
  "$AAPT2" dump badging "$APK" | sed -n '1,3p'
fi

if command -v sha256sum >/dev/null 2>&1; then
  sha256sum "$APK"
else
  shasum -a 256 "$APK"
fi
