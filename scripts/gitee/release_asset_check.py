"""Gitee Go 流水线脚本：检查最新 Release 是否三件套齐全

只依赖 Python 标准库，失败默认不阻断流水线（除非显式 --strict）。
"""

import os, sys, json, urllib.request

REPO = os.environ.get('GITEE_REPO', 'RXHrxh520/APIBalance')
TOKEN = os.environ.get('GITEE_TOKEN', '')
API = 'https://gitee.com/api/v5/repos/' + REPO + '/releases'

q = API + '?per_page=1' + ('&access_token=' + TOKEN if TOKEN else '')
try:
    rels = json.loads(urllib.request.urlopen(
        urllib.request.Request(q, headers={'User-Agent': 'ci'}), timeout=30).read())
except Exception as e:
    print('拉取发布列表失败:', e)
    sys.exit(0)

if not rels:
    print('仓库还没有发布')
    sys.exit(0)

rel = rels[0]
names = [a['name'] for a in rel.get('assets', [])]
print('最新发布:', rel['tag_name'])
for n in names:
    print('  -', n)
missing = [k for k in ('APK', 'source.zip', 'SHA256SUMS') if not any(k.lower() in n.lower() for n in names)]
if missing:
    print('缺少:', ', '.join(missing))
else:
    print('三件套齐全')
sys.exit(0)
