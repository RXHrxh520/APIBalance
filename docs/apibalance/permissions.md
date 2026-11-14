---
layout: default
title: 权限说明 · APIBalance
description: APIBalance 申请的每一项 Android 权限分别用来做什么，以及不授权会影响哪些功能。
permalink: /apibalance/permissions.html
---

[← 返回 APIBalance 文档](./)

# 权限说明

安装后会遇到系统弹窗索要权限。这里逐项说明**为什么要、不批会怎样**。原则只有一条：只申请真正会用到的那几项，不批也能用主功能。

| 权限 | 用来做什么 | 不授权的后果 |
|---|---|---|
| **网络访问**（INTERNET / ACCESS_NETWORK_STATE） | 请求各家官方余额 API；检查更新 | 无法查询任何余额，App 只能看历史 |
| **通知**（POST_NOTIFICATIONS，Android 13+） | 余额低于阈值提醒、定时刷新结果、常驻余额通知 | 不弹出提醒，查询结果只在打开 App 时可见 |
| **悬浮窗**（SYSTEM_ALERT_WINDOW） | 悬浮球在桌面直接显示余额数字 | 只影响悬浮球，其他功能正常 |
| **开机与后台运行**（RECEIVE_BOOT_COMPLETED / 前台服务） | 定时自动刷新到点执行、重启后恢复刷新计划 | 定时刷新不触发，需手动打开 App 才查询 |
| **存储/下载**（MediaStore 写入「下载」目录） | 历史导出 CSV / JSON、备份导出 `.bin` `.zip`、保存余额卡片图 | 无法导出文件，其余功能正常 |
| **指纹/生物识别**（USE_BIOMETRIC） | 打开 App 时的指纹解锁 | 只能改用密码锁 |
| **安装未知应用**（用户手动开启，非权限） | 应用内检查更新后直接装 APK | 需自己去 Releases 下载安装 |

## 这些权限不会被用来做什么

- **不上传**你的 API Key。密钥经 Android Keystore 加密后只存在本机 SharedPreferences。
- **不读**通讯录、短信、通话记录、相册（除你主动选图当背景）。
- **不做**后台静默联网上报。请求只发往各家官方 API 与 GitHub 版本检查。
- **不含**广告 SDK，不含第三方统计/埋点 SDK。

## 只想最小授权怎么办

只给「网络」就能用：查询、历史、趋势、主题全部可用。通知、悬浮窗、定时刷新都是可选增强，随时可以在系统设置里补授权，App 会重新检测。

> 完整的数据流向说明见 [隐私与安全](security.html)，仓库根目录另有 [隐私声明](https://github.com/RXHrxh520/APIBalance/blob/main/PRIVACY.md)。
