#!/usr/bin/env bash
# 版本号升位工具：同时更新 versionCode 与 versionName（macOS/Linux 通用，不依赖 sed -i）。
# 用法：./scripts/bump-version.sh [patch|minor|major] [gradle文件路径]
set -euo pipefail

TYPE="${1:-patch}"
GRADLE_FILE="${2:-app/build.gradle}"

if [ ! -f "$GRADLE_FILE" ]; then
  echo "找不到 $GRADLE_FILE，请在仓库根目录执行本脚本" >&2
  exit 1
fi

python3 - "$GRADLE_FILE" "$TYPE" <<'PY'
import re
import sys
from pathlib import Path

path = Path(sys.argv[1])
kind = sys.argv[2]
text = path.read_text(encoding="utf-8")

m_code = re.search(r"versionCode\s+(\d+)", text)
m_name = re.search(r'''versionName\s+([\'"])([^\'"]+)\1''', text)
if not m_code or not m_name:
    sys.exit(f"未能在 {path} 中找到 versionCode / versionName")

old_code = int(m_code.group(1))
quote = m_name.group(1)
old_name = m_name.group(2)
parts = [int(p) if p.isdigit() else 0 for p in old_name.replace("-", ".").split(".")]
while len(parts) < 3:
    parts.append(0)
if kind == "major":
    parts = [parts[0] + 1, 0, 0]
elif kind == "minor":
    parts = [parts[0], parts[1] + 1, 0]
else:
    parts = [parts[0], parts[1], parts[2] + 1]
new_code = old_code + 1
new_name = ".".join(str(p) for p in parts)

text = text[: m_code.start()] + f"versionCode {new_code}" + text[m_code.end():]
text = text[: m_name.start()] + f'versionName {quote}{new_name}{quote}' + text[m_name.end():]
path.write_text(text, encoding="utf-8")
print(f"versionCode : {old_code} -> {new_code}")
print(f"versionName : {old_name} -> {new_name}")
PY

echo
echo "下一步：./gradlew assembleRelease，然后把 APK 与签名信息发到 Release"
