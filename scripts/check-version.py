#!/usr/bin/env python3
"""检查版本号一致性：build.gradle、发布清单、脚本示例、README 不能各写一套版本。"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BUILD = ROOT / "app/build.gradle"
text = BUILD.read_text(encoding="utf-8")
code = re.search(r"versionCode\s+(\d+)", text)
name = re.search(r'versionName\s+[\'"]([^\'"]+)[\'"]', text)
if not code or not name:
    sys.exit("app/build.gradle 缺少 versionCode / versionName")

version = name.group(1)
vc = code.group(1)
failures = []
print(f"versionName={version} versionCode={vc}")

checks = {
    "docs/release-checklist.md": [version],
    "scripts/README.md": [version],
}
for rel, needles in checks.items():
    path = ROOT / rel
    if not path.exists():
        continue
    body = path.read_text(encoding="utf-8")
    for needle in needles:
        if needle not in body:
            failures.append(f"{rel}: 没有出现当前版本 {needle}")

# 发布清单里的示例版本应等于当前版本。
checklist = ROOT / "docs/release-checklist.md"
if checklist.exists():
    body = checklist.read_text(encoding="utf-8")
    for found in re.findall(r"\b1\.\d+\.\d+\b", body):
        if found != version:
            failures.append(f"docs/release-checklist.md: 出现过期版本 {found}（当前 {version}）")

# 不要在 XML 资源里硬编码 1.x.y 版本号。
for path in (ROOT / "app/src/main/res").glob("values*/strings.xml"):
    body = path.read_text(encoding="utf-8")
    for found in re.findall(r"\b1\.\d+\.\d+\b", body):
        if found != version:
            failures.append(f"{path.relative_to(ROOT)}: 硬编码版本 {found}（当前 {version}）")

if failures:
    print("版本一致性检查失败：")
    for item in sorted(set(failures)):
        print(" -", item)
    sys.exit(1)
print("版本一致性检查通过")
