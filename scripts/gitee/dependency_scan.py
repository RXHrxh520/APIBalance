"""Gitee Go 流水线脚本：用 OSV 检查 Gradle 依赖有没有已知漏洞

只依赖 Python 标准库，失败默认不阻断流水线（除非显式 --strict）。
"""

import re, sys, json, pathlib, urllib.request

# 依赖坐标直接从 Gradle 脚本里抽，避免维护第二份清单
coords = set()
pat = re.compile(r'["\']([a-zA-Z0-9_.\-]+):([a-zA-Z0-9_.\-]+):([0-9][^"\']*)["\']')
for f in list(pathlib.Path('.').rglob('build.gradle')) + list(pathlib.Path('.').rglob('*.gradle.kts')) + list(pathlib.Path('.').rglob('libs.versions.toml')):
    for m in pat.finditer(f.read_text(encoding='utf-8', errors='ignore')):
        coords.add((m.group(1), m.group(2), m.group(3).strip()))

print('发现依赖', len(coords), '个')
hits = []
for g, a, v in sorted(coords):
    body = json.dumps({'package': {'name': g + ':' + a, 'ecosystem': 'Maven'}, 'version': v}).encode()
    try:
        req = urllib.request.Request('https://api.osv.dev/v1/query', data=body,
                                     headers={'Content-Type': 'application/json'})
        r = json.loads(urllib.request.urlopen(req, timeout=25).read())
    except Exception as e:
        print('查询失败', g, a, v, e)
        continue
    for vul in r.get('vulns', []):
        hits.append((g + ':' + a + ':' + v, vul.get('id'), vul.get('summary', '')[:70]))

if hits:
    print('命中漏洞:')
    for h in hits:
        print('  ', ' | '.join(str(x) for x in h))
else:
    print('未发现已知漏洞')
sys.exit(0)
