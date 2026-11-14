---
layout: default
title: 发版同步清单 · APIBalance
description: 每次发布新版本时，哪些文件、页面和仓库位置必须一起更新。
permalink: /release-checklist.html
---

# 发版同步清单

这份清单解决一个具体问题：**每次发版，Wiki、README、文档站、协议文本散落在四五个地方，凭记忆改一定会漏。**
下面按「先改什么、再改什么」的顺序排，照着走一遍就行。

---

## 一、版本号与代码（必改）

| 位置 | 改什么 |
|---|---|
| `app/build.gradle` | `versionCode` 加 1（只增不减，回退版本会导致系统拒装） |
| `app/build.gradle` | `versionName` 改成对外版本号，如 `1.5.4` |
| `app/src/main/res/values/strings.xml` | 版本号展示用的字符串（如有独立条目） |

改完先本地编译一次，确认能出包再往下走——后面所有页面都要引用这个版本号。

---

## 二、四种语言（必查）

| 语言 | 文件 |
|---|---|
| 简体中文 | `app/src/main/res/values/strings.xml` |
| English | `app/src/main/res/values-en/strings.xml` |
| 繁體中文 | `app/src/main/res/values-zh-rTW/strings.xml` |
| 萌语 | `app/src/main/res/values-eo/strings.xml` |

新增任何一条文案，**四份文件都要加**，键名必须一致。缺键会静默回退成中文，界面里就冒出一句中英混杂的怪话。

自检命令（在仓库根目录执行）：

```bash
python3 - <<'PY'
import re
def keys(p):
    s = open(p, encoding='utf-8').read()
    return set(re.findall(r'<string name="([^"]+)"', s))
a = keys('app/src/main/res/values/strings.xml')
for loc in ('en', 'zh-rTW', 'eo'):
    b = keys(f'app/src/main/res/values-{loc}/strings.xml')
    print(loc, '缺失', sorted(a - b), '多余', sorted(b - a))
PY
```

四行输出都应该是 `缺失 [] 多余 []`。

---

## 三、README（必改）

| 内容 | 说明 |
|---|---|
| 版本徽章 | `img.shields.io/badge/版本-v1.5.x` 这类静态徽章要跟着改 |
| 下载链接 | Release 页地址不变，但文案里的版本号要同步 |
| 功能列表 | 本次新增/删除的功能条目 |
| 截图 | 界面有大改才换，小改不必重拍 |

---

## 四、文档站 `docs/`

| 文件 | 什么时候改 |
|---|---|
| `docs/features.md` | 功能有增删改 |
| `docs/build.md` | 源码结构变动（新增/删除 Java 文件） |
| `docs/errors.md` | 新增了错误码或改了提示文案 |
| `docs/faq.md` | 用户反复问的新问题 |
| `docs/roadmap.md` | 计划项完成或调整 |
| `docs/permissions.md` | 权限申请有变化 |
| `docs/security.md` | 加密、备份、日志策略有变化 |
| `docs/errors.md` / `docs/index.md` | 首页入口表有新增页面时补链接 |

推送后跑一次文档质量检查（工作流会自己跑，也可本地先跑）：

```bash
python3 scripts/check-docs.py
```

---

## 五、Wiki（必改）

Wiki 是独立仓库，**不随主仓库推送自动更新**，必须手动克隆、改、推：

```bash
git clone https://github.com/RXHrxh520/APIBalance.wiki.git /tmp/wiki
```

| 页面 | 什么时候改 |
|---|---|
| `Home.md` | 概述、下载入口、版本号 |
| `服务商支持矩阵.md` | 服务商增删、接口或计费方式变化 |
| `版本与下载说明.md` | 每个版本必改：写清版本号、日期、主要变化、下载地址 |
| `常见问题索引.md` | 新问题归档 |
| `开发与贡献约定.md` | 提交规范、代码结构有变 |
| `发布流程.md` | 流程本身变了才改 |
| `_Sidebar.md` | 新增 Wiki 页面时补一行 |

---

## 六、协议与隐私文本（按需改）

| 位置 | 内容 |
|---|---|
| `app/src/main/res/values*/strings.xml` 中的用户协议、隐私政策条目 | 条款有实质变化才改；四语言同步 |
| `PRIVACY.md` | 数据收集范围、权限用途有变化 |
| `docs/permissions.md` | 同上 |

**判定标准**：只要新增了任何联网、读存储、读设备的权限，就必须改这三处，不能只改一处。

---

## 七、发布（必做）

1. GitHub Release：打 tag `v1.5.x`，上传 APK，正文写「新增 / 修复 / 变更」三段。
2. QQ 群资源发布界面同步一份更新说明（仓库不放 CHANGELOG）。
3. Release 里必须带 APK，否则 `release-asset-check` 工作流会自动开 Issue。
4. 发布后确认：
   - `Actions` 里 `docs-quality` 是绿的；
   - Release 附件能下载；
   - 文档站首页能打开。

---

## 八、一次性自检（发布后 10 分钟）

- [ ] 四个语言包键位齐平（第二节的命令输出为空）
- [ ] README 版本徽章与新版本一致
- [ ] Wiki「版本与下载说明」已写本版本
- [ ] Release 带 APK
- [ ] 文档站 9 个页面都能打开
- [ ] 关于页的版本号与 `versionName` 一致

---

返回：[GitHub 仓库](https://github.com/RXHrxh520/APIBalance) · [构建与源码结构](build.html)
