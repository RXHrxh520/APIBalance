#!/usr/bin/env sh
# 发布产物打包：构建、重命名、生成 SHA-256。
set -eu
cd "$(dirname "$0")/.."

VERSION=$(sed -nE "s/.*versionName[[:space:]]*['\"]([^'\"]*)['\"].*/\1/p" app/build.gradle | head -1)
[ -n "$VERSION" ] || { echo "无法从 app/build.gradle 读取 versionName" >&2; exit 1; }

./build-release.sh "$@"

SRC="app/build/outputs/apk/release/app-release.apk"
[ -f "$SRC" ] || { echo "未找到构建产物：$SRC" >&2; exit 1; }

mkdir -p dist
OUT="dist/APIBalance-${VERSION}.apk"
cp "$SRC" "$OUT"
if command -v sha256sum >/dev/null 2>&1; then
  sha256sum "$OUT" > "$OUT.sha256"
else
  shasum -a 256 "$OUT" > "$OUT.sha256"
fi
echo "已生成 $OUT"
echo "已生成 $OUT.sha256"
