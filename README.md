# APIBalance（已更名）

> 多服务商 API 余额查询安卓工具 · 开源免费 · AGPL-3.0

一个支持 **DeepSeek / Kimi / 智谱 GLM / Anthropic / 火山方舟 / Kimi Code / GLM Coding / Codex** 的 Android 余额与套餐配额查询工具。密钥经系统级加密（Android Keystore + AES/GCM）仅保存在本机，不经过任何私人服务器，只调用各家官方 API。
- **快速下载**：<https://github.com/RXHrxh520/APIBalance/releases/latest>
- **项目地址**：<https://github.com/RXHrxh520/APIBalance>
- **交流群**：235492286

![version](https://img.shields.io/badge/version-1.x-brightgreen?labelColor=555)
---


## 📲 安装方式

1. 从 Releases 下载最新 APK（APIBalance_vx.x.apk）
2. 在手机上允许"安装未知来源应用"
3. 安装后打开，输入 API Key 即可查询

> 签名说明：正式版使用作者签名（*.keystore）。若从源码自编译，签名会不同，无法覆盖安装官方版，属正常现象。安全中心会据此提示是否与官方证书一致，仅做提醒。

---

## ✨ 功能总览

### 余额查询
- 多服务商：DeepSeek / Kimi / 智谱GLM / Anthropic / 火山方舟 / Kimi Code / GLM Coding / Codex，统一查询界面
- 自适应展示：金额余额显示总额/赠金/充值；成本接口显示累计成本；套餐接口显示 5 小时与每周剩余，不把配额伪装成金额
- 负余额：欠费显示真实负值，不再被四舍五入成 0.00 或 -0.00
- 多币种显示：自动识别 / CNY / USD 切换（离线近似汇率换算，支持自定义汇率 JSON 地址）
- 余额异常提醒：低于阈值发状态栏通知
- 定时自动刷新：按间隔后台查询并通知（可自定义秒级）
- 通知栏常驻余额（可选）
- 静音时段：设定时间段内提醒不响铃
- 底部导航：余额 / 趋势 / 设置 三 Tab 快捷切换
- 返回逻辑：有上一级回上一级；首页返回/侧滑需二次确认，避免误退到桌面

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
- Android SDK 37（platform android-37，build-tools 37.0.0）
- JDK 27
- Gradle 9.8.2（AGP 9.4.0）
- 如需同款 ARM64架构构建包，请前往 QQ 群文件下载build.zip GitHub 无法上传大文件

### 构建命令
```bash
./gradlew assembleRelease
```
产出：`app/build/outputs/apk/release/app-release.apk`

> 说明：工程已显式开启 `buildConfig`。若本机是 arm64 架构，需自备 arm64 版 aapt2，并追加
> `-Pandroid.aapt2FromMavenOverride=<SDK>/build-tools/37.0.0/aapt2` 与 `-Pandroid.builder.sdkDownload=false`。

### 签名
签名信息不保存在仓库中。构建时按 keystore.properties、环境变量、`-P` 参数的顺序读取
RXH_STORE_FILE / RXH_STORE_PASS / RXH_KEY_ALIAS / RXH_KEY_PASS，
口令缺失会直接报错终止，不会产出未签名或错误签名的包。开源分发时请使用你自己的密钥库。

---

## 🔒 隐私与安全

- **密钥存储**：Android Keystore 生成 AES-256 密钥，AES/GCM 加密后存 SharedPreferences，明文不落盘（自动登录 Key 与多账户 Key 均加密）
- **网络**：仅请求各家官方余额 API（api.deepseek.com / api.moonshot.ai / open.bigmodel.cn / api.anthropic.com）与 GitHub 版本检查；TLS 证书校验恒开启
- **备份包**：导出前用你设定的密码加密，恢复需同一密码；恢复代码修改需先验证锁屏密码
- **无任何私人服务器**，无广告 SDK，无第三方统计

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

## 📄 开源协议

本项目基于 **GNU Affero General Public License v3.0** 开源。

- 本软件永远开源免费。如您是付费购买的，请联系对应方退款。
- 仅调用各家服务商官方余额 API，仅供学习交流使用，作者不承担滥用 API 导致的任何账号风险。
- 图标说明：App 图标为Deepseek的萌化版形象；QQ 图标取自系统素材；GitHub 图标为 GitHub 官方 Octicons（MIT 许可）；其余图标为 Material Design Icons 风格（Apache 2.0）。

---

## 🖼 截图

（待补充：欢迎贡献截图）

---


## 📜 版本历史
- 为保证说明书整洁性，自1.5.3版本起，此处将不再提供历史版本更新公告，如需查看请前往Github讨论区 QQ群 资源发布界面查看!

---

## ⚠️ 免责声明

- 本项目仅供学习交流使用，请勿用于任何违法用途
- 使用本项目查询余额不产生额外费用；但调用各家 API 会产生 token 消耗，请自行注意
- 作者不承担因滥用 API、泄露 API Key 导致的任何账号风险与损失
- 本软件永远开源免费；如您是付费购买的，请联系对应方退款

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

## 📮 联系方式

- 项目仓库：https://github.com/RXHrxh520/APIBalance
- 交流群：235492286（QQ）
- 作者个人QQ:406992556

> 欢迎 Star、Fork、提 Issue，让 APIBalance 变得更好。
