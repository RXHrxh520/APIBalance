#!/usr/bin/env python3
"""检查语言包的 string 与 string-array 是否与 base 完整对齐。缺 key 会静默回退中文。"""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BASE = ROOT / "app/src/main/res/values/strings.xml"
LOCALES = [
    "values-en",
    "values-zh-rTW",
    "values-b+zh+MEOW",
    "values-eo",
]


def load(path: Path):
    root = ET.parse(path).getroot()
    strings = {}
    arrays = {}
    for node in root:
        if node.tag == "string":
            strings[node.get("name")] = "".join(node.itertext())
        elif node.tag in ("string-array", "array", "integer-array"):
            arrays[node.get("name")] = ["".join(item.itertext()) for item in node.findall("item")]
    return strings, arrays


def main() -> int:
    base_strings, base_arrays = load(BASE)
    print(f"base: {len(base_strings)} strings, {len(base_arrays)} arrays")
    failed = False
    for loc in LOCALES:
        path = ROOT / "app/src/main/res" / loc / "strings.xml"
        if not path.exists():
            print(f"{loc}: 文件不存在")
            failed = True
            continue
        strings, arrays = load(path)
        missing_s = sorted(set(base_strings) - set(strings))
        extra_s = sorted(set(strings) - set(base_strings))
        missing_a = sorted(set(base_arrays) - set(arrays))
        extra_a = sorted(set(arrays) - set(base_arrays))
        print(f"{loc}: {len(strings)} strings, {len(arrays)} arrays, "
              f"missing {len(missing_s)}+{len(missing_a)}, extra {len(extra_s)}+{len(extra_a)}")
        for label, items in (("missing string", missing_s), ("extra string", extra_s),
                             ("missing array", missing_a), ("extra array", extra_a)):
            if items:
                print(f"  {label}:", ", ".join(items[:12]))
                failed = True
        for name, items in base_arrays.items():
            if name in arrays and len(arrays[name]) != len(items):
                print(f"  array {name}: {len(arrays[name])} items, expected {len(items)}")
                failed = True
        if loc == "values-eo":
            empty = [k for k, v in strings.items() if not v.strip()]
            if empty:
                print("  empty eo strings:", ", ".join(empty[:12]))
                failed = True
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
