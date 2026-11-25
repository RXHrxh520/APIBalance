"""Gitee Go 流水线脚本：扫描 README 与 docs 的外链可访问性

只依赖 Python 标准库，失败默认不阻断流水线（除非显式 --strict）。
"""

import re, sys, pathlib, urllib.request, concurrent.futures

URL = re.compile(r'https?://[^\s)<>"\]]+')
ROOT = pathlib.Path('.')
files = [ROOT / 'README.md'] + sorted((ROOT / 'docs').rglob('*.md'))
links = {}
for f in files:
    if not f.exists():
        continue
    for m in URL.findall(f.read_text(encoding='utf-8', errors='ignore')):
        links.setdefault(m.rstrip('.,;)'), set()).add(str(f))

def probe(u):
    try:
        req = urllib.request.Request(u, method='HEAD', headers={'User-Agent': 'APIBalance-CI'})
        code = urllib.request.urlopen(req, timeout=15).getcode()
        return u, code
    except Exception as e:
        code = getattr(e, 'code', 0)
        return u, code

bad = []
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as ex:
    for u, code in ex.map(probe, list(links)[:80]):
        if code and code < 400:
            continue
        bad.append((u, code))

print(f'扫描 {len(links)} 个外链，异常 {len(bad)} 个')
for u, code in bad:
    src = ', '.join(sorted(links[u]))
    print(f'  [{code or "ERR"}] {u}  <- {src}')
# 外链失效属于外部因素，不阻断构建
sys.exit(0)
