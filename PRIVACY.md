# 隐私声明

APIBalance 是一个只查询余额的本地工具。这份声明说明它**拿到什么、存在哪、发到哪**。

## 一句话版本

你的 API Key 加密后只存在你自己的手机上；App 只调用各家服务商的官方接口查余额，不经过任何中间服务器，不含广告与统计 SDK。

## 1. 收集哪些信息

| 数据 | 是否收集 | 存放位置 | 说明 |
|---|---|---|---|
| API Key / Secret | 是 | 本机（加密） | Android Keystore 生成 AES-256 密钥，AES/GCM 加密后写入 SharedPreferences，明文不落盘 |
| 余额与配额数值 | 是 | 本机 | 用于历史记录、趋势图、小组件 |
| 查询时间、Token 估算 | 是 | 本机 | 用于趋势与用量统计 |
| 设备标识、IMEI、位置、通讯录 | 否 | — | 完全不读取 |
| 使用行为、崩溃埋点 | 否 | — | 无第三方统计 SDK；崩溃日志仅在你手动查看调试页时本地生成 |

## 2. 数据发往哪里

只发往以下域名，且只为完成查询与更新检查：

- `api.deepseek.com`（DeepSeek 余额）
- `api.moonshot.ai` / Moonshot 官方域名（Kimi 余额）
- `open.bigmodel.cn`（智谱 GLM 余额）
- `api.anthropic.com`（Anthropic 组织成本）
- 火山方舟官方接口（账户余额）
- `api.github.com` / `github.com`（检查更新、下载新版 APK）
- 如你自行填写了「自定义汇率 JSON 地址」，则额外请求该地址

**不存在**任何私人中转服务器，没有自建后端，没有云端同步。

## 3. 权限

每项权限的用途与影响见 [权限说明](docs/apibalance/permissions.md)。核心只有「网络」一项是必需的，其余都是可选增强。

## 4. 备份与导出

- 备份包（`.bin` / `.zip`）在导出前用你设定的密码加密，恢复需同一密码；
- 修改恢复代码前需先通过锁屏密码验证；
- 导出的 CSV / JSON 只写到本机「下载」目录，不上传。

## 5. 第三方组件

- 图标：Material Design Icons 风格（Apache 2.0）；GitHub 图标为 Octicons（MIT）；
- 图表：Canvas 自绘，未引入图表库；
- 无广告 SDK、无统计 SDK、无推送服务商 SDK。

## 6. 你的控制权

- 随时在 App 内删除已保存的 Key 与历史记录；
- 卸载 App 即清除本机全部数据（导出备份除外）；
- 可直接查看源码核实上述行为：<https://github.com/RXHrxh520/APIBalance>

## 7. 变更

本声明如有修改，会随提交更新在本文件体现，不另行通知。

> 最后更新：2026-11
