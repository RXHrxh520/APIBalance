#!/usr/bin/env python3
"""检查许可证一致性：Java 头必须是 BSD-3-Clause，源码/资源里不能残留 AGPL。"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "app/src/main/java"
RES = ROOT / "app/src/main/res"
BAD = re.compile(r"AGPL|Affero|GNU Affero", re.I)
SPDX = re.compile(r"SPDX-License-Identifier:\s*BSD-3-Clause")

failures = []

java_files = list(JAVA.rglob("*.java")) + list((ROOT / "app/src/test").rglob("*.java"))
for path in sorted(java_files):
    text = path.read_text(encoding="utf-8")
    if not SPDX.search(text):
        failures.append(f"{path}: 缺少 BSD-3-Clause SPDX 头")

for path in sorted(RES.glob("values*/strings.xml")):
    text = path.read_text(encoding="utf-8")
    for match in BAD.finditer(text):
        line = text.count("\n", 0, match.start()) + 1
        failures.append(f"{path}:{line}: 残留 {match.group(0)}")

for rel, prefix in [
    ("LICENSE", "BSD 3-Clause License"),
    ("app/src/main/LICENSE", "BSD 3-Clause License"),
    ("app/src/main/assets/licenses/APIBalance-LICENSE.txt", "BSD 3-Clause License"),
    ("app/src/main/assets/licenses/Apache-2.0.txt", "Apache License"),
    ("app/src/main/assets/licenses/THIRD_PARTY_NOTICES.txt", "APIBalance"),
]:
    path = ROOT / rel
    if not path.exists():
        failures.append(f"{rel}: 文件不存在")
    elif not path.read_text(encoding="utf-8").lstrip().startswith(prefix):
        failures.append(f"{rel}: 开头不是 {prefix}")

if failures:
    print("许可证检查失败：")
    for item in failures:
        print(" -", item)
    sys.exit(1)

print("许可证检查通过：Java 头、语言包、APK 内置许可文本")
