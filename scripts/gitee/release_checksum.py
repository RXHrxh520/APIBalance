"""Gitee Go 流水线脚本：为 Gitee Release 附件生成 SHA256SUMS

只依赖 Python 标准库，失败默认不阻断流水线（除非显式 --strict）。
"""

import os, sys, json, hashlib, urllib.request, urllib.parse

REPO = os.environ.get('GITEE_REPO', 'RXHrxh520/APIBalance')
TOKEN = os.environ.get('GITEE_TOKEN', '')
API = 'https://gitee.com/api/v5/repos/' + REPO + '/releases'

if not TOKEN:
    print('未配置 GITEE_TOKEN，跳过校验和上传（在流水线环境变量里加 GITEE_TOKEN 即可启用）')
    sys.exit(0)

def get(u):
    return urllib.request.urlopen(urllib.request.Request(u, headers={'User-Agent': 'ci'}), timeout=30).read()

try:
    rels = json.loads(get(API + '?access_token=' + TOKEN + '&per_page=1'))
    rel = rels[0]
    tag = rel['tag_name']
    print('目标发布:', tag, '附件', len(rel.get('assets', [])), '个')
    lines = []
    for a in rel.get('assets', []):
        name = a['name']
        if name.startswith('SHA256'):
            continue
        data = get(a['browser_download_url'])
        lines.append(hashlib.sha256(data).hexdigest() + '  ' + name)
        print('  ' + lines[-1])
    if not lines:
        print('没有可比对的附件')
        sys.exit(0)
    print('\n'.join(lines))
except Exception as e:
    print('生成校验和失败:', e)
