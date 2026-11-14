---
layout: default
title: APIBalance · 安卓多服务商 AI API 余额查询
description: 一个安卓 App 查完 DeepSeek / Kimi / 智谱 GLM / Anthropic / 火山方舟 / Kimi Code / GLM Coding / Codex 的余额与套餐配额，密钥本地加密。
---

# APIBalance

**一个安卓 App，查完 8 家 AI 的余额和套餐配额。** 密钥只存在你自己的手机里，不上传任何服务器。

**[⬇ 下载最新版 APK](https://github.com/RXHrxh520/APIBalance/releases/latest)** &nbsp;·&nbsp; [GitHub 仓库](https://github.com/RXHrxh520/APIBalance) &nbsp;·&nbsp; QQ 群：235492286

---

## 文档目录

| 文档 | 内容 |
|---|---|
| **[完整功能清单](features.html)** | 10 个分类、57 项功能的逐条说明 |
| **[安装与常见问题](faq.html)** | 装不上、报 401/429、后台不提醒、换手机迁移 |
| **[隐私与安全](security.html)** | 密钥怎么存、请求发到哪、备份怎么加密 |
| **[构建与源码结构](build.html)** | 自己编译、签名说明、目录结构 |

## 它解决什么问题

你在 DeepSeek 充过钱、在 Kimi 充过钱、在 Claude 也充过钱。想看还剩多少，得一个个登录后台翻——8 家就是 8 个后台。

APIBalance 把这 8 家塞进一个界面：打开就看完所有余额、赠金、套餐剩余，还能看趋势、设提醒、放桌面小组件。它只读余额，不转发请求、不做代理、不碰对话内容。

## 支持的服务商

| 服务商 | 能查到什么 |
|---|---|
| DeepSeek | 总额 / 赠金 / 充值，欠费显示真实负值 |
| Kimi（Moonshot） | 账户余额 |
| 智谱 GLM | 账户余额（新旧格式 Key 都兼容） |
| Anthropic（Claude） | 组织累计成本 |
| 火山方舟 | 账户余额（AK/SK 签名） |
| Kimi Code / GLM Coding / Codex | 5 小时 · 每周剩余配额 |

## 三步上手

1. **装**：下载 APK，允许"安装未知来源应用"，覆盖安装保留原有数据。
2. **填 Key**：各家控制台 → API Keys → 新建 → 粘贴进 App，可保存多个命名账户。
3. **看**：打开即余额；想要更省事就开定时刷新、桌面小组件或悬浮球。

## 功能亮点

- 8 家一个界面，统一查询与展示
- 负余额显示真实欠费值；套餐显示周期剩余；多币种自动识别 CNY / USD
- 密钥用 Android Keystore + AES/GCM 加密，明文不落盘
- 余额低于阈值就提醒，可设静音时段
- 趋势折线图 + 月度柱状图，近 7/30/90 天，可导出 CSV / JSON
- 桌面小组件、悬浮球、长按图标一键查余额
- 指纹 / 密码锁、隐藏余额、禁止截屏、禁通知
- 3 套界面风格、8 种强调色、深色模式、自定义背景与文案

## 安全承诺

密钥不上传任何服务器；只请求各家官方余额接口与 GitHub 版本检查，TLS 校验恒开启；无广告 SDK、无第三方统计。详细说明见 [隐私与安全](security.html)。

---

<small>本项目基于 AGPL-3.0 开源，永久免费。如你是付费购买的，请联系卖方退款。</small>

<div style="text-align:center;margin-top:24px">
  <img src="https://hits.sh/github.com/RXHrxh520/APIBalance.svg?label=页面访问" alt="页面访问计数" height="20" loading="lazy" onerror="this.style.display='none'">
  <br>
  <img src="https://count.getloli.com/get/@RXHrxh520.APIBalance?theme=moebooru" alt="访客计数器" height="90" loading="lazy" onerror="this.style.display='none'">
</div>
