---
layout: default
title: 公告配置 · APIBalance
description: app.json 公告字段说明，改完立即生效，不需要发版。
permalink: /announcement.html
---

# 公告配置（app.json）

公告直接读仓库根目录的 `app.json`，**改完推到 main 就生效**，不用发新版、不用等构建。

## 三分钟上手（照着改就行）

公告只有一件事：**在 `app.json` 里放一段 JSON，让余额页顶部显示一句话**。你不需要写代码，只要改这三个地方：

1. 打开仓库根目录的 `app.json`；
2. 把下面这段整个复制进去；
3. 改 `text` 里的字，推送到 `main`，下拉刷新余额页就能看到。

```json
{
  "_readme": "下划线开头的键只是写给你自己看的，程序会忽略",
  "notices": [
    {
      "id": "my-first-notice",
      "enabled": true,
      "text": "11 月 20 日凌晨 2 点会短时维护，余额查询可能失败。",
      "url": "https://github.com/RXHrxh520/APIBalance/releases/latest",
      "start": "2026-11-15",
      "end": "2026-11-20"
    }
  ]
}
```

你真正需要改的只有这 5 个字段：

| 你想做什么 | 改哪里 | 例子 |
|---|---|---|
| 改公告内容 | `text` | `"周末愉快，余额查询正常。"` |
| 临时关掉公告 | `enabled` 改成 `false` | `"enabled": false` |
| 今天开始显示 | `start` | `"2026-11-15"` |
| 到某天自动消失 | `end` | `"2026-11-20"`，留空 `""` 表示一直显示 |
| 点击公告跳转 | `url` | 必须是 `https://` 开头的地址，留空就不跳 |

### 三个常见场景

**只想挂一句话，不跳转：**

```json
{"notices":[{"text":"今晚 23:00 后恢复查询。","url":"","start":"2026-11-15","end":""}]}
```

**同时准备两条，只显示当前有效的：**

```json
{
  "notices": [
    {"text": "双十一活动公告", "start": "2026-11-01", "end": "2026-11-12"},
    {"text": "日常公告", "start": "2026-11-13", "end": ""}
  ]
}
```

程序从上往下找第一条「`enabled` 不是 false、并且今天在 `start`/`end` 之间」的公告；前面的过期了会自动用下一条。

**写注释：** JSON 本身不支持 `//` 注释。想给自己留说明，就加 `"_readme": "这里写说明"`，下划线开头的键程序全部忽略。

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
