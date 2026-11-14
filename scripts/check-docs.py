#!/usr/bin/env python3
"""文档站自检：front matter、permalink 唯一性、站内链接、弃用域名。

在仓库根目录运行：python3 scripts/check-docs.py
任何一项失败都会以非零状态退出，供 CI 使用。
"""
import os, re, sys

DOCS = "docs"
BAD_HOST = "rxh.asia"
FM = re.compile(r"^\ufeff?---\n(.*?)\n---\n", re.S)
LINK = re.compile(r"\]\((/[^)\s]+\.html)\)")

errors, permalinks, files = [], {}, []

for root, _, names in os.walk(DOCS):
    for n in names:
        if n.endswith(".md"):
            files.append(os.path.join(root, n))

if not files:
    sys.exit(f"未在 {DOCS}/ 下找到任何 .md 文件")

for path in files:
    text = open(path, encoding="utf-8").read()
    m = FM.match(text)
    if not m:
        errors.append(f"{path}: 缺少 front matter")
        continue
    fm = m.group(1)
    if "layout:" not in fm:
        errors.append(f"{path}: front matter 缺少 layout")
    if "title:" not in fm:
        errors.append(f"{path}: front matter 缺少 title")
    pm = re.search(r"permalink:\s*(\S+)", fm)
    if pm:
        if pm.group(1) in permalinks:
            errors.append(f"{path}: permalink {pm.group(1)} 与 {permalinks[pm.group(1)]} 重复")
        permalinks[pm.group(1)] = path
    if BAD_HOST in text:
        errors.append(f"{path}: 出现已停用的域名 {BAD_HOST}")

known = set(permalinks) | {"/"}
for path in files:
    text = open(path, encoding="utf-8").read()
    for link in set(LINK.findall(text)):
        if link not in known:
            errors.append(f"{path}: 站内链接 {link} 没有对应页面")

# GitHub Pages 必须存在 404 页
if "/404.html" not in known:
    errors.append("docs: 缺少 permalink: /404.html 的页面")

print(f"检查 {len(files)} 个页面，permalink {len(permalinks)} 个")
if errors:
    print("\n发现问题：")
    for e in errors:
        print(" -", e)
    sys.exit(1)
print("全部通过")
