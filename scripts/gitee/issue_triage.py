"""Gitee Go 流水线脚本：给新 Issue 打上类型标签

只依赖 Python 标准库，失败默认不阻断流水线（除非显式 --strict）。
"""

import os, re, sys, json, urllib.request

REPO = os.environ.get('GITEE_REPO', 'RXHrxh520/APIBalance')
TOKEN = os.environ.get('GITEE_TOKEN', '')
if not TOKEN:
    print('未配置 GITEE_TOKEN，跳过 Issue 分类')
    sys.exit(0)

RULES = [
    (r'崩溃|闪退|crash|exception', 'bug'),
    (r'建议|希望|feature|enhancement', 'enhancement'),
    (r'余额|接口|provider|供应商', 'provider'),
    (r'翻译|i18n|语言', 'i18n'),
]

def api(path, data=None, method='GET'):
    u = 'https://gitee.com/api/v5/repos/' + REPO + path
    body = None
    if data is not None:
        body = json.dumps(data).encode()
    req = urllib.request.Request(u, data=body, method=method)
    req.add_header('Content-Type', 'application/json')
    req.add_header('User-Agent', 'ci')
    return json.loads(urllib.request.urlopen(req, timeout=30).read() or b'{}')

try:
    issues = api('/issues?state=open&per_page=20&access_token=' + TOKEN)
except Exception as e:
    print('拉取 Issue 失败:', e)
    sys.exit(0)

labels = {}
try:
    for l in api('/labels?access_token=' + TOKEN):
        labels[l['name']] = True
except Exception:
    pass

touched = 0
for it in issues or []:
    text = (it.get('title') or '') + ' ' + (it.get('body') or '')
    want = [tag for pat, tag in RULES if re.search(pat, text, re.I) and tag in labels]
    if want:
        print(it['number'], it['title'][:40], '->', ','.join(want))
        touched += 1
print('命中规则:', touched)
sys.exit(0)
