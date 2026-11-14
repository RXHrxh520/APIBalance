#!/usr/bin/env bash
# 版本号升位工具：同时更新 versionCode 与 versionName
# 用法：./scripts/bump-version.sh [patch|minor|major] [gradle文件路径]
set -euo pipefail

TYPE="${1:-patch}"
GRADLE_FILE="${2:-app/build.gradle}"

if [ ! -f "$GRADLE_FILE" ]; then
  echo "找不到 $GRADLE_FILE，请在仓库根目录执行本脚本" >&2
  exit 1
fi

OLD_CODE=$(grep -oE 'versionCode[[:space:]]+[0-9]+' "$GRADLE_FILE" | head -1 | grep -oE '[0-9]+')
OLD_NAME=$(grep -oE 'versionName[[:space:]]+"[^"]+"' "$GRADLE_FILE" | head -1 | sed -E 's/.*"([^"]+)".*/\1/')

if [ -z "$OLD_CODE" ] || [ -z "$OLD_NAME" ]; then
  echo "未能在 $GRADLE_FILE 中找到 versionCode / versionName" >&2
  exit 1
fi

NEW_CODE=$((OLD_CODE + 1))
NEW_NAME=$(python3 - "$OLD_NAME" "$TYPE" <<'PY'
import sys
old, kind = sys.argv[1], sys.argv[2]
parts = [int(p) if p.isdigit() else 0 for p in old.replace('-', '.').split('.')]
while len(parts) < 3:
    parts.append(0)
if kind == 'major':
    parts = [parts[0] + 1, 0, 0]
elif kind == 'minor':
    parts = [parts[0], parts[1] + 1, 0]
else:
    parts = [parts[0], parts[1], parts[2] + 1]
print('.'.join(str(p) for p in parts))
PY
)

sed -i -E "s/versionCode[[:space:]]+${OLD_CODE}/versionCode ${NEW_CODE}/" "$GRADLE_FILE"
sed -i -E "s/versionName[[:space:]]+\"${OLD_NAME//./\\.}\"/versionName \"${NEW_NAME}\"/" "$GRADLE_FILE"

echo "versionCode : ${OLD_CODE} -> ${NEW_CODE}"
echo "versionName : ${OLD_NAME} -> ${NEW_NAME}"
echo
echo "下一步：./gradlew assembleRelease，然后把 APK 与签名信息发到 Release"
