# 构建、签名与源码结构

> 面向想自己编译的开发者。由原说明书原样迁移。

## 构建

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

## 目录结构

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

返回：[项目主页](../README.md) · [完整功能清单](features.md)
