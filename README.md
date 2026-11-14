# APIBalance

**一个安卓 App，查完 8 家 AI 的余额和套餐配额。**

DeepSeek · Kimi · 智谱 GLM · Anthropic · 火山方舟 · Kimi Code · GLM Coding · Codex

密钥只存在你自己的手机里，不上传任何服务器。

**[⬇ 下载最新版 APK](https://github.com/RXHrxh520/APIBalance/releases/latest)** &nbsp;·&nbsp; [完整功能清单](docs/features.md) &nbsp;·&nbsp; [安装与常见问题](docs/faq.md) &nbsp;·&nbsp; QQ 群：235492286

![Stars](https://img.shields.io/github/stars/RXHrxh520/APIBalance?style=social)
![Downloads](https://img.shields.io/github/downloads/RXHrxh520/APIBalance/total?style=social)
![Release](https://img.shields.io/github/v/release/RXHrxh520/APIBalance?label=最新版本&color=brightgreen)
![License](https://img.shields.io/github/license/RXHrxh520/APIBalance?color=brightgreen)
![Platform](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/语言-中文%20%7C%20English%20%7C%20繁體%20%7C%20萌语-blue)
![Last Commit](https://img.shields.io/github/last-commit/RXHrxh520/APIBalance?color=ff69b4)

---

## 这是干什么用的

你在 DeepSeek 充过钱、在 Kimi 充过钱、在 Claude 也充过钱。想看还剩多少，得一个个登录后台翻——8 家就是 8 个后台。

**APIBalance 把这 8 家塞进一个界面**：打开就看完所有余额、赠金、套餐剩余，还能看趋势、设提醒、放桌面小组件。

它只做一件事：**读余额**。不转发你的请求、不做代理、不碰你的对话内容。

## 支持的服务商

| 服务商 | 能查到什么 | Key 类型 |
|---|---|---|
| **DeepSeek** | 总额 / 赠金 / 充值，欠费显示真实负值 | API Key |
| **Kimi（Moonshot）** | 账户余额 | API Key |
| **智谱 GLM** | 账户余额（新旧格式 Key 都兼容） | API Key |
| **Anthropic（Claude）** | 组织累计成本 | Admin Key |
| **火山方舟** | 账户余额 | AccessKey / SecretKey |
| **Kimi Code** | 5 小时 / 每周剩余配额 | 订阅套餐 |
| **GLM Coding** | 5 小时 / 每周剩余配额 | 订阅套餐 |
| **Codex** | 套餐配额 | 订阅套餐 |

> 金额类显示"还剩多少钱"，成本类显示"已经花了多少"，套餐类显示"这个周期还剩多少"——**按接口真实含义显示，不把配额伪装成金额**。

## 三步上手

1. **装**：点上面的下载按钮，允许"安装未知来源应用"，覆盖安装即可（保留原有数据）。
2. **填 Key**：各家控制台 → API Keys → 新建 → 复制粘贴进 App。多个账号可以分别命名保存（"工作号""个人号"）。
3. **看**：打开就是余额。想要更省事，去设置里开定时自动刷新、桌面小组件或悬浮球。

> 不知道 Key 在哪拿？看 [常见问题](docs/faq.md)。

## 功能亮点

- **8 家一个界面**：统一查询、统一展示，不用来回切后台。
- **不糊弄数字**：负余额显示真实欠费值；套餐显示周期剩余；多币种自动识别 CNY / USD。
- **密钥本地加密**：Android Keystore + AES/GCM，密钥不出硬件，明文不落盘。
- **余额掉了会提醒**：低于阈值发通知，可设静音时段，不半夜吵你。
- **能看趋势**：折线图 + 月度柱状图，近 7 / 30 / 90 天消耗一目了然，可导出 CSV / JSON。
- **桌面就能看**：小组件、悬浮球、长按图标"一键查余额"，不打开 App 也知道剩多少。
- **锁得住**：指纹 / 密码锁、隐藏余额（\*\*\*\*）、禁止截屏、禁通知，三开关独立。
- **能自己装修**：3 套界面风格、8 种强调色、深色模式、自定义背景与文案、4 种语言（含萌语）。

<details>
<summary>展开看全部 57 项功能（完整清单另见 docs/features.md）</summary>

完整清单已迁移到 [docs/features.md](docs/features.md)，条目与原说明书一字未改，按 10 个分类整理，共 57 项：余额查询、安全隐私、历史与统计、多账户、主题中心、界面版本、网络与工具、用户引导、调试。

</details>

## 截图

> 待补充。上传 3 张图到 `assets/screenshots/`（建议命名 `balance.png`、`trend.png`、`theme.png`），然后把下面这段的注释去掉即可。

```markdown
| 余额主界面 | 趋势图 | 主题中心 |
|---|---|---|
| ![余额](assets/screenshots/balance.png) | ![趋势](assets/screenshots/trend.png) | ![主题](assets/screenshots/theme.png) |
```

## 为什么可以放心用

- **密钥不上传**：只存本机，用系统级 Android Keystore 加密，任何服务器都拿不到。
- **只连官方域名**：仅请求各家官方余额接口与 GitHub 版本检查，TLS 校验恒开启，无广告 SDK、无第三方统计。
- **能识别假包**：安全中心内置官方证书指纹，被重新签名的仿冒包会当场报出实际指纹。
- **随时能搬走**：备份导出 `.bin` / `.zip`（密码加密），换手机一键还原；也能彻底卸载干净。

详见 [隐私与安全](docs/security.md)。

## 常见问题（简版）

| 问题 | 答案 |
|---|---|
| 提示"签名不一致"装不上 | 先卸载旧版再装（**先备份**，卸载会清密钥） |
| 余额显示 0 / -0.00 | 老版本 bug，升到 v1.5.3 已修复 |
| 报 401 / 403 | Key 有空格、过期，或需要走代理 |
| 报 429 | 被限流，等几分钟，把自动刷新间隔调长 |
| 关掉 App 不提醒了 | 安卓省电策略，允许后台活动 + 自启动 |
| 换手机怎么迁移 | 备份导出 → 新机导入同一密码恢复 |

更多见 [完整常见问题](docs/faq.md)。

## 📚 完整文档

| 我想… | 去哪看 |
|---|---|
| 看全部功能（57 项） | [docs/features.md](docs/features.md) |
| 装不上、报错、看不懂 | [docs/faq.md](docs/faq.md) |
| 了解密钥怎么加密 | [docs/security.md](docs/security.md) |
| 自己编译 / 签名 / 源码结构 | [docs/build.md](docs/build.md) |
| 参与贡献、翻译 | [CONTRIBUTING.md](CONTRIBUTING.md) |

## 参与贡献

- 提 Issue：报告 Bug、建议新功能（仓库已内置模板）
- 提 PR：修问题、优化代码
- 翻译：完善 `values/values-en`、`values-zh-rTW`、`values-eo` 等多语言

开发约定：新增 Java 文件必须带 AGPL 版权头；不引入第三方统计/广告 SDK；不擅自修改各家 API 端点。

## 📄 开源协议

本项目基于 **GNU Affero General Public License v3.0** 开源。

- 本软件永远开源免费。如您是付费购买的，请联系对应方退款。
- 仅调用各家服务商官方余额 API，仅供学习交流使用，作者不承担滥用 API 导致的任何账号风险。
- 图标说明：App 图标为 Deepseek 的萌化版形象；QQ 图标取自系统素材；GitHub 图标为官方 Octicons（MIT）；其余图标为 Material Design Icons 风格（Apache 2.0）。

## ⚠️ 免责声明

- 本项目仅供学习交流使用，请勿用于任何违法用途。
- 查询余额本身不产生额外费用；但调用各家 API 会产生 token 消耗，请自行注意。
- 作者不承担因滥用 API、泄露 API Key 导致的任何账号风险与损失。

## 📮 联系方式

- 项目仓库：<https://github.com/RXHrxh520/APIBalance>
- 交流群：235492286（QQ）
- 作者 QQ：406992556

> 觉得有用的话，点个 Star 就是最大的支持 ⭐

<details>
<summary>📊 访客统计（第三方图，加载不出来不影响内容）</summary>

![访问计数](https://hits.sh/github.com/RXHrxh520/APIBalance.svg?label=页面访问)

<img src="https://count.getloli.com/get/@RXHrxh520.APIBalance?theme=moebooru" alt="二次元访客计数器" height="90" loading="lazy" />

</details>
