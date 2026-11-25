"""Gitee Go 流水线脚本：巡检官网与文档站可用性

只依赖 Python 标准库，失败默认不阻断流水线（除非显式 --strict）。
"""

import sys, urllib.request

SITES = ['https://rxh.im/', 'https://rxh.im/docs/', 'https://gitee.com/RXHrxh520/APIBalance']
fail = 0
for u in SITES:
    try:
        r = urllib.request.urlopen(urllib.request.Request(u, headers={'User-Agent': 'ci'}), timeout=20)
        print(f'[OK  {r.getcode()}] {u}')
    except Exception as e:
        fail += 1
        print(f'[ERR     ] {u}  {e}')
print('异常站点:', fail)
sys.exit(0)
