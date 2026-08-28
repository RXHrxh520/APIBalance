# APIBalance（已更名）

> 多服务商 API 余额查询安卓工具 · 开源免费 · AGPL-3.0

一个支持 **DeepSeek / Kimi (Moonshot) / 智谱 GLM / OpenAI / Anthropic** 五家大模型服务商余额查询的 Android 应用。密钥经系统级加密（Android Keystore + AES/GCM）仅保存在本机，不经过任何私人服务器，只调用各家官方余额 API。

**项目地址**：<https://github.com/RXHrxh520/APIBalance>
**交流群**：235492286

---

## ✨ 功能总览

### 余额查询
- 多服务商：DeepSeek / Kimi / 智谱GLM / OpenAI / Anthropic，统一查询界面
- 多币种显示：自动识别 / CNY / USD 切换（离线近似汇率换算，支持自定义汇率 JSON 地址）
- 余额异常提醒：低于阈值发状态栏通知
- 定时自动刷新：按间隔后台查询并通知（可自定义秒级）
- 通知栏常驻余额（可选）
- 静音时段：设定时间段内提醒不响铃
- 底部导航：余额 / 趋势 / 设置 三 Tab 快捷切换

### 安全隐私
- 指纹/密码锁：打开 App 需验证（支持离开 1/5/15 分钟自动锁）
- 隐藏余额：以 **** 显示
- 隐私三开关：禁止截屏 / 禁用通知 / 隐藏余额（独立开关）
- API Key 加密存储：Android Keystore + AES/GCM，密钥不出硬件

### 历史与统计
- 历史余额记录（最近 200 条）
- 趋势折线图 + 月度柱状图（Canvas 自绘，无第三方库）
- 按服务商筛选 / 按时间范围过滤（近 7/30/90 天）
- 月度统计：记录数 / 均值 / 最低 / 最高
- 历史导出：CSV / JSON（系统分享）
- Token 用量记录：按天累计估算 token 消耗

### 多账户
- 同一服务商可保存多个命名 Key（如"工作号""个人号"）
- 一键切换已保存账户（支持重命名 / 删除）

### 主题中心
- 三套主题：默认 / **复古2000（千禧年霓虹风）** / **Code CLI（终端风，荧光绿+等宽字体）**
- 随心选色：8 种强调色（紫/绿/蓝/橙/粉/青/黄/红）
- 深色/浅色/跟随系统切换（真生效，非表面）+ 定时自动深色
- 自定义背景：从相册选图作为背景
- 字体大小：小 / 标准 / 大
- 语言切换：中文 / English / 繁體中文 / 跟随系统

### 网络与工具
- 超时/重试设置
- 网络代理（自定义主机/端口）
- 自定义 User-Agent
- 请求日志（logcat）
- Token 估算器（估算文本 token 数，自动记入用量）
- 检查更新（GitHub Release 检测，支持应用内下载安装 APK）
- 桌面小组件：多尺寸、显示余额与赠金/充值明细、一键刷新
- 桌面快捷方式：长按图标「一键查余额」
- 悬浮球：桌面直显余额数字，点击即后台刷新
- 余额分享：生成品牌格式余额卡片图，分享/保存相册
- 峰谷价趣味卡片：梁文峰（工作日峰时）/ 梁文谷（其余+周末谷时 5 折）
- 第三方依赖统计：如实列出语言/构建链/服务商 API/许可

### 调试
- 分级崩溃测试（6 种异常类型，脱敏堆栈报告查看）
- 网络连通性测试（五家服务商域名）
- 服务商 API 端点探测
- 加密存储自检 / 历史自检 / 权限检查 / 系统信息

---

## 📦 构建

### 环境要求
- Android SDK 34 (platform android-34, build-tools 34.0.0)
- JDK 17
- Gradle 7.5（AGP 7.4.2）

### 构建命令
```bash
./gradlew assembleRelease
```
产出：`app/build/outputs/apk/release/app-release.apk`

> 说明：工程使用标准 Maven 依赖（AGP 7.4.2 从 google() 拉取），不依赖任何私有工具链。若本机是 arm64 架构，需自备 arm64 版 aapt2（见 `gradle.properties` 的 `android.aapt2FromMavenOverride`）。

### 签名
工程内置签名配置指向 `RXH.keystore`（别名 `rxh`）。开源分发时建议更换为你自己的密钥库。签名信息可通过环境变量覆盖（`RXH_KEYSTORE` / `RXH_STORE_PASS` / `RXH_KEY_PASS`）。

---

## 📁 目录结构

```
app/src/main/java/asia/rxh/apibalance/
├── MainActivity.java          # 主入口：底部导航/网络/主题/隐私/定时刷新
├── LoginFragment.java         # 登录：服务商选择 + 人机验证
├── BalanceFragment.java       # 余额页：查询/汇率/涨跌动效/峰谷卡片/分享入口
├── SettingsFragment.java      # 设置：主题/隐私/网络/账户/工具/语言
├── HistoryFragment.java       # 历史：趋势图/柱状图/筛选/统计/导出
├── ShareFragment.java         # 余额分享：品牌格式卡片图生成
├── AboutFragment.java         # 关于：联系方式/检查更新
├── DependenciesFragment.java  # 第三方依赖统计
├── DebugFragment.java         # 调试工具（14 项）
├── LockFragment.java          # 指纹/密码锁
├── ProviderConfig.java        # 服务商配置 + 响应解析（5 家）
├── CryptoManager.java         # API Key 加密存储
├── AccountManager.java        # 多账户管理
├── BalanceHistoryStore.java   # 历史记录存储
├── NetworkClient.java         # 统一网络层（代理/超时/重试/TLS）
├── ThemeUtils.java            # 主题应用（深色全覆盖）
├── TrendChartView.java        # 趋势折线图（Canvas）
├── TrendBarChartView.java     # 月度柱状图（Canvas）
├── ThemePrefs.java            # 主题中心
├── PrivacyPrefs.java          # 隐私设置
├── AppPrefs.java              # 通用设置
├── Notifier.java              # 通知
├── AutoRefreshReceiver.java   # 定时刷新
├── UpdateChecker.java         # 检查更新 + 应用内更新
├── TokenEstimator.java        # Token 估算
├── TokenUsageStore.java       # Token 用量记账
├── FxRateFetcher.java         # 自定义汇率拉取（URL + JSON）
├── PeakOffpeak.java           # 峰谷价
├── BalanceWidgetProvider.java # 桌面小组件
├── FloatingWindowService.java # 悬浮球
├── UiDialog.java              # 自定义弹窗
└── CrashHandler/CrashActivity # 崩溃捕获
```

---

## 🔒 隐私与安全

- **密钥存储**：Android Keystore 生成 AES-256 密钥，AES/GCM 加密后存 SharedPreferences，明文不落盘（自动登录 Key 与多账户 Key 均加密）
- **网络**：仅请求各家官方余额 API（api.deepseek.com / api.moonshot.ai / open.bigmodel.cn / api.openai.com / api.anthropic.com）与 GitHub 版本检查；TLS 证书校验恒开启
- **无任何私人服务器**，无广告 SDK，无第三方统计

---

## 📄 开源协议

本项目基于 **GNU Affero General Public License v3.0** 开源。

- 本软件永远开源免费。如您是付费购买的，请联系对应方退款；如拒不退款，请联系项目原作者协助（无法保证百分百）。
- 仅调用各家服务商官方余额 API，仅供学习交流使用，作者不承担滥用 API 导致的任何账号风险。
- 图标说明：App 图标为作者提供的萌化版形象；QQ 图标取自系统素材；GitHub 图标为 GitHub 官方 Octicons（MIT 许可）；其余图标为 Material Design Icons 风格（Apache 2.0）。

---

## 📲 安装方式

1. 从 Releases 下载最新 APK（APIBalance_v1.x.apk）
2. 在手机上允许"安装未知来源应用"
3. 安装后打开，输入 API Key 即可查询

> 签名说明：正式版使用作者签名（RXH.keystore）。若从源码自编译，签名会不同，无法覆盖安装官方版，属正常现象。

---

## 🖼 截图

（待补充：欢迎贡献截图）

---

## 🤝 贡献指南

欢迎任何形式的贡献：

- 提 Issue：报告 Bug、建议新功能
- 提 PR：修复问题、优化代码
- 翻译：完善 values/values-en/values-zh-rTW 等多语言

开发约定：
- 所有新增 Java 文件必须带 AGPL 版权头（见现有文件）
- 不引入第三方统计/广告 SDK
- 不擅自修改各家 API 端点

---

## 📜 版本历史

### v1.3.1（当前）
- 服务商扩展至五家：DeepSeek / Kimi / 智谱GLM / OpenAI / Anthropic
- 底部导航重构（余额/趋势/设置三 Tab）
- 自定义汇率：填写 JSON 地址拉取（约定 {"usd_cny": 7.2}）
- 悬浮球余额直显 + 点击后台刷新
- 历史导出 CSV/JSON、月度柱状图
- Token 用量按天记录
- 定时自动深色、定时自动锁（1/5/15 分钟）
- 桌面小组件增强：多尺寸 + 刷新按钮 + 明细
- 应用内更新（下载 APK 直接安装）
- 桌面快捷方式「一键查余额」
- 语言：新增繁體中文，完整双语资源化
- 余额分享页（品牌格式卡片图）
- 第三方依赖统计页
- 移除 TLS 校验开关（恒开启）
- 全量安全加固：多账户 Key 加密、清除数据补全

### v1.3
- 品牌格式余额分享、语言切换（中/英）
- 启动静默检查更新（数字版本比较）
- 全部布局与动态文案双语资源化（300+ 条）
- 调试页扩充至 14 项（分级崩溃测试/脱敏堆栈/自检）
- UI 全面优化：圆角聚焦框、图标体系、主题中心三步合一、功能弹窗说明

### v1.2
- 多服务商：DeepSeek / Kimi / 智谱GLM
- API Key 加密存储（Keystore + AES/GCM）
- 指纹/密码锁、隐藏余额、隐私三开关
- 余额提醒 + 阈值、定时刷新（可配秒级间隔）、通知栏常驻 + 静音时段 + 刷新 Action + 关键操作通知
- 历史记录：趋势图、按服务商/时间筛选、月度统计
- 多账户（命名保存多个 Key）
- 主题中心：默认 / 复古2000 / Code CLI + 8 色随心选
- 自定义背景、字体大小
- 网络：代理、TLS 开关、自定义 UA、请求日志
- Token 估算器、检查更新、桌面小组件、悬浮球、峰谷价卡片
- 调试工具：网络测试 / API 探测 / Key 检测 / 清缓存 / 查看设置

### v1.1
- 多服务商雏形、暗黑模式、历史记录、动画库

### v1.0 (BETA)
- DeepSeek 单服务商余额查询

---

## ⚠️ 免责声明

- 本项目仅供学习交流使用，请勿用于任何违法用途
- 使用本项目查询余额不产生额外费用；但调用各家 API 会产生 token 消耗，请自行注意
- 作者不承担因滥用 API、泄露 API Key 导致的任何账号风险与损失
- 本软件永远开源免费；如您是付费购买的，请联系对应方退款

---

## 📮 联系方式

- 项目仓库：https://github.com/RXHrxh520/APIBalance
- 交流群：235492286（QQ）

> 欢迎 Star、Fork、提 Issue，让 APIBalance 变得更好。

