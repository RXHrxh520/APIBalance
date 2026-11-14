---
layout: default
title: 公告配置 · APIBalance
description: app.json 公告字段说明，改完立即生效，不需要发版。
permalink: /announcement.html
---

# 公告配置（app.json）

公告直接读仓库根目录的 `app.json`，**改完推到 main 就生效**，不用发新版、不用等构建。

## 新格式（推荐）

```json
{
  "_readme": "说明文字，程序会忽略所有以下划线开头的键",
  "notices": [
    {
      "id": "welcome-2026-11",
      "enabled": true,
      "text": "这里是要显示在余额页的那句话",
      "url": "https://github.com/RXHrxh520/APIBalance/releases/latest",
      "start": "2026-11-14",
      "end": ""
    }
  ]
}
```

字段含义：

| 字段 | 必填 | 说明 |
|---|---|---|
| `id` | 否 | 随便起个名字，方便你自己认，程序不用 |
| `enabled` | 否 | `false` 就跳过这条，默认 `true` |
| `text` | 是 | 公告正文，纯文本，不支持 HTML |
| `url` | 否 | 点公告跳转的地址，留空就只显示不跳转 |
| `start` | 否 | 开始日期，`yyyy-MM-dd`，留空表示从现在开始 |
| `end` | 否 | 结束日期，`yyyy-MM-dd`，留空表示一直显示 |

显示规则：**从上往下取第一条「启用且今天在 start/end 之间」的公告**。
想临时下掉某条，把 `enabled` 改成 `false` 就行，不用删。

## 老格式（仍然兼容）

```json
{
  "notice": "一句话公告",
  "notice_url": "https://example.com"
}
```

`notice` 也可以写成对象：`{"text": "...", "url": "..."}`。仓库现在同时保留这个字段作为兜底。

## 注意事项

1. **JSON 不能写 `//` 注释**，写了整份文件就解析失败，公告会退回上一条缓存。要写说明就用 `_readme` 这类下划线键。
2. 日期只比字符串，格式必须是 `yyyy-MM-dd`，写成 `2026/11/14` 不会生效。
3. 改完建议本地校验一次：

```bash
python3 -c "import json;json.load(open('app.json'));print('OK')"
```

4. 公告会缓存在本地，断网时显示上次成功拉取的内容；删掉 `notices` 且 `notice` 为空时，公告条会隐藏。
