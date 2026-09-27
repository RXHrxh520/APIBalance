# APIBalance（已更名）

> 多服务商 API 余额查询安卓工具 · 开源免费 · AGPL-3.0

一个支持 **DeepSeek / Kimi (Moonshot) / 智谱 GLM / Anthropic** 四家大模型服务商余额查询的 Android 应用。密钥经系统级加密（Android Keystore + AES/GCM）仅保存在本机，不经过任何私人服务器，只调用各家官方余额 API。

**项目地址**：<https://github.com/RXHrxh520/APIBalance>
**交流群**：235492286

---

## ✨ 功能总览

### 余额查询
- 多服务商：DeepSeek / Kimi / 智谱GLM / Anthropic，统一查询界面
- 多币种显示：自动识别 / CNY / USD 切换（离线近似汇率换算，支持自定义汇率 JSON 地址）
- 余额异常提醒：低于阈值发状态栏通知
- 定时自动刷新：按间隔后台查询并通知（可自定义秒级）
- 通知栏常驻余额（可选）
- 静音时段：设定时间段内提醒不响铃
- 底部导航：余额 / 趋势 / 设置 三 Tab 快捷切换
- 返回逻辑：有上一级回上一级，栈空了才退出，不再强行跳回余额页

### 安全隐私
- 指纹/密码锁：打开 App 需验证（支持离开 1/5/15 分钟自动锁）
- 锁定状态记忆：切换主题/语言重新构建界面不再重复弹密码
- 隐藏余额：以 **** 显示
- 隐私三开关：禁止截屏 / 禁用通知 / 隐藏余额（独立开关）
- API Key 加密存储：Android Keystore + AES/GCM，密钥不出硬件
- 安全中心：逐项体检（签名 / 完整性 / 安装来源 / 悬浮窗 / 调试器 / Root 等），共 20 项
  - 签名校验对比**内置官方证书指纹**，被重新签名的仿冒包会直接报出实际指纹
  - APK 完整性校验运行中的代码是否来自本安装包
  - 安装来源显示真实包名，读不到就明说侧载
- 备份与恢复：导出 `.bin` / `.zip`（密码加密），还原含 API Key
- 恢复代码：修改前需先通过锁屏密码验证

### 历史与统计
- 历史余额记录（最近 200 条）
- 趋势折线图 + 月度柱状图（Canvas 自绘，无第三方库）
- 按服务商筛选 / 按时间范围过滤（近 7/30/90 天）
- 月度统计：记录数 / 均值 / 最低 / 最高
- 清重复：同一服务商 + 数值完全相同 + 时间相差 10 分钟内才判定为重复，保留正常的持平历史
- 历史导出：CSV / JSON，直接写入「下载」目录并回显完整路径
- Token 用量记录：按天累计估算 token 消耗

### 多账户
- 同一服务商可保存多个命名 Key（如"工作号""个人号"）
- 一键切换已保存账户（支持重命名 / 删除）

### 主题中心
- 界面风格：默认 / **复古2000（千禧年霓虹风）** / **Code CLI（终端风，荧光绿+等宽字体）**
- 随心选色：8 种强调色（紫/绿/蓝/橙/粉/青/黄/红），**全局生效**（按钮、选中态、图标同步）
- 深色/浅色/跟随系统切换（真生效，非表面）+ 定时自动深色
- 自定义背景：从相册选图作为背景
- 自定义图标
- 文案自定义：余额卡片那句俏皮话可自己写 / 从 24 句内置文案里挑 / 自定义分享模板
- 字体大小：小 / 标准 / 大
- 语言切换：中文 / English / 繁體中文 / **萌语** / 跟随系统

### 界面版本
- 旧版 / 新版设置**双形态**，默认进入旧版，设置里可随时切换
- 切换时清空回退栈，不会出现新旧页面叠加

### 网络与工具
- 超时/重试设置
- 网络代理（自定义主机/端口）
- 自定义 User-Agent
- 请求日志（logcat）
- Token 估算器（估算文本 token 数，自动记入用量）
- 检查更新（GitHub Release 检测，支持应用内下载安装 APK）
  - 下载时有**进度弹窗**（进度条 + 百分比），完成后自动调起安装
- 桌面小组件：多尺寸、显示余额与赠金/充值明细、一键刷新
- 桌面快捷方式：长按图标「一键查余额」
- 悬浮球：桌面直显余额数字，点击即后台刷新
- 余额分享：生成品牌格式余额卡片图，分享/保存相册
- 峰谷价趣味卡片：梁文峰（工作日峰时）/ 梁文谷（其余+周末谷时 5 折）
- 第三方依赖统计：如实列出语言/构建链/服务商 API/许可

### 用户引导
- 首次启动分步引导（服务商 / Key / 个性化 / 界面版本）
- 专业模式**当场答题**：打开开关立即弹题，答对才真正开启，答错保持关闭

### 卸载
- 卸载入口直接跳转「应用详情」页，由用户自行确认卸载

### 调试
- 分级崩溃测试（6 种异常类型，脱敏堆栈报告查看）
- 网络连通性测试（五家服务商域名）
- 服务商 API 端点探测
- 加密存储自检 / 历史自检 / 权限检查 / 系统信息
- 强制重置彩蛋状态（一键清空彩蛋记录与隐藏主题标记）

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
├── MainActivity.java            # 主入口：底部导航/网络/主题/隐私/定时刷新
├── OnboardingActivity.java      # 用户引导（分步 + 专业模式当场答题）
├── LoginFragment.java           # 登录：服务商选择 + 人机验证
├── BalanceFragment.java         # 余额页：查询/汇率/涨跌动效/峰谷卡片/分享入口
├── SettingsFragment.java        # 设置：主题/隐私/网络/账户/工具/语言
├── SettingsHomeFragment.java    # 新版设置首页（分类入口）
├── ThemeCenterActivity.java     # 主题中心：风格/强调色/个性化/文案
├── SecurityCenterActivity.java  # 安全中心（20 项体检）
├── BackupActivity.java          # 备份与恢复（BIN/ZIP + 密码加密）
├── UninstallActivity.java       # 卸载引导（跳转应用详情）
├── HistoryFragment.java         # 历史：趋势图/柱状图/筛选/统计/导出
├── ShareFragment.java           # 余额分享：品牌格式卡片图生成
├── AboutFragment.java           # 关于：联系方式/检查更新
├── MineFragment.java            # 我的：个人信息/贡献者/开发人员/彩蛋
├── DependenciesFragment.java    # 第三方依赖统计
├── DebugFragment.java           # 调试工具（15 项）
├── LockFragment.java            # 密码锁 / 指纹锁
├── ProviderConfig.java          # 服务商配置 + 响应解析
├── CryptoManager.java           # API Key 加密存储
├── AccountManager.java          # 多账户管理
├── BalanceHistoryStore.java     # 历史记录存储 + 清重复
├── NetworkClient.java           # 统一网络层（代理/超时/重试/TLS）
├── Exporter.java                # 统一导出到「下载」目录（MediaStore）
├── EnvChecker.java              # 环境与安全检查项
├── ThemeUtils.java              # 主题应用（深色全覆盖 + 强调色）
├── TrendChartView.java          # 趋势折线图（Canvas）
├── TrendBarChartView.java       # 月度柱状图（Canvas）
├── ThemePrefs.java              # 主题中心
├── PrivacyPrefs.java            # 隐私设置
├── AppPrefs.java                # 通用设置
├── ProQuiz.java                 # 专业模式答题（随机抽题）
├── EggChat.java                 # 彩蛋对话
├── Notifier.java                # 通知
├── AutoRefreshReceiver.java     # 定时刷新
├── UpdateChecker.java           # 检查更新 + 应用内更新（带进度）
├── TokenEstimator.java          # Token 估算
├── TokenUsageStore.java         # Token 用量记账
├── FxRateFetcher.java           # 自定义汇率拉取（URL + JSON）
├── PeakOffpeak.java             # 峰谷价
├── BalanceWidgetProvider.java   # 桌面小组件
├── FloatingWindowService.java   # 悬浮球
├── UiDialog.java                # 自定义弹窗
└── CrashHandler/CrashActivity   # 崩溃捕获
```

---

## 🔒 隐私与安全

- **密钥存储**：Android Keystore 生成 AES-256 密钥，AES/GCM 加密后存 SharedPreferences，明文不落盘（自动登录 Key 与多账户 Key 均加密）
- **网络**：仅请求各家官方余额 API（api.deepseek.com / api.moonshot.ai / open.bigmodel.cn / api.anthropic.com）与 GitHub 版本检查；TLS 证书校验恒开启
- **备份包**：导出前用你设定的密码加密，恢复需同一密码；恢复代码修改需先验证锁屏密码
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

> 签名说明：正式版使用作者签名（RXH.keystore）。若从源码自编译，签名会不同，无法覆盖安装官方版，属正常现象。安全中心会据此提示是否与官方证书一致。

---

## 🖼 截图

（待补充：欢迎贡献截图）

---

## 🤝 贡献指南

欢迎任何形式的贡献：

- 提 Issue：报告 Bug、建议新功能
- 提 PR：修复问题、优化代码
- 翻译：完善 values/values-en/values-zh-rTW/values-eo 等多语言

开发约定：
- 所有新增 Java 文件必须带 AGPL 版权头（见现有文件）
- 不引入第三方统计/广告 SDK
- 不擅自修改各家 API 端点

---

## 📜 版本历史

### v1.4（当前）
- 四家服务商：DeepSeek / Kimi / 智谱GLM / Anthropic（OpenAI 从选择列表移除）
- 语言新增**萌语**（`values-eo`），补齐英文/繁体缺失条目
- 新增**用户引导**页：分步引导 + 专业模式当场答题（题库 20 道随机抽 5，选项每次打乱）
- 新增**安全中心**：20 项体检；签名对比内置官方证书指纹，可识别重签名仿冒包
- 新增**备份与恢复**：BIN/ZIP 导出、密码加密、含 API Key 还原
- 新增**主题中心**独立页：界面风格 / 8 种强调色（全局生效）/ 自定义背景与图标 / 文案自定义（俏皮话 + 分享模板）
- 新增**旧版/新版设置**双形态，默认旧版，可随时切换（切换时隔离回退栈）
- 新增**彩蛋**：点三次进入彩蛋对话，隐藏主题与粉色主题
- 新增**调试项**：强制重置彩蛋状态
- 新增桌面**卸载入口**改为跳转应用详情
- 应用内更新新增**下载进度弹窗**
- 历史新增**清重复**（时间窗口判定，不误删持平记录）
- 历史导出改为直接落盘「下载」目录并回显路径
- 统一导出走 MediaStore（`Exporter`），失败自动退回公共下载目录
- 密码锁移除「系统原生验证」选项，仅保留软件密码
- 修复：切换主题/语言重建界面会重复弹密码
- 修复：专业模式答题通过后不生效、专业模式状态前后矛盾
- 修复：界面风格名被峰时文案覆盖
- 修复：强调色只在设置页生效
- 修复：清重复清不掉真实重复数据
- 修复：旧版设置返回键/侧边栏可能卡出新版设置
- 修复：贡献者弹窗显示字面量 "null"
- 修复：安全中心悬浮窗项自相矛盾（App 自己申请的权限被当风险）
- 修复：备份密码不足 4 位时静默无提示
- 修复：恢复代码可直接修改，现需先验证锁屏密码
- 权限清单清理：`USE_FINGERPRINT` → `USE_BIOMETRIC`，`READ_EXTERNAL_STORAGE` 加 `maxSdkVersion=32`，移除 `READ_MEDIA_IMAGES`，补 `<queries>` 声明
- 返回逻辑修复：不再无条件跳回余额页
- 移除 ">" 装饰符，统一列表样式

### v1.3.1
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
