# 脚本目录

存放构建、发版与维护用的小工具，全部本地执行，不依赖外部服务。

## 构建与检查

| 脚本 | 用途 | 用法 |
|---|---|---|
| `check-env.sh` | 检查 JDK 27+、SDK 37、build-tools 37.0.0、aapt2；`--release` 时再查签名 | `./scripts/check-env.sh --release` |
| `check-env.ps1` | 同上，Windows PowerShell 版 | `powershell -ExecutionPolicy Bypass -File scripts/check-env.ps1 --release` |
| `build-debug.sh` / `.bat` / `.ps1` | 调试构建，不需要 release keystore；ARM64 自动加 aapt2 参数 | `./scripts/build-debug.sh` |
| `build-release.sh` / `.bat` / `.ps1` | 发布构建，先做环境与签名检查，ARM64 自动加 aapt2 参数 | `./build-release.sh` |
| `package-release.sh` | 构建 release、复制为 `dist/APIBalance-<version>.apk` 并生成 `.sha256` | `./scripts/package-release.sh` |
| `verify-apk.sh` | 校验 APK 签名证书、zipalign、版本信息与 SHA-256 | `./scripts/verify-apk.sh [apk路径]` |
| `generate-keystore.sh` | 生成 release keystore 并打印证书 SHA-256 | `./scripts/generate-keystore.sh key0 keystore.jks 口令` |

## 版本与发布

| 脚本 | 用途 | 用法 |
|---|---|---|
| `bump-version.sh` | 同步升位 versionCode 与 versionName，macOS/Linux 通用 | `./scripts/bump-version.sh patch`（或 `minor` / `major`） |
| `bump-version.ps1` / `.bat` | 同上，Windows 版 | `powershell -ExecutionPolicy Bypass -File scripts/bump-version.ps1 -Type patch` |
| `release-checksum.sh` | 为已发布 Release 生成并上传 `SHA256SUMS.txt`（APK + 源码包） | `GH_TOKEN=... ./scripts/release-checksum.sh [tag]` |

## 质量检查

| 脚本 | 用途 | 用法 |
|---|---|---|
| `check-i18n.py` | 检查五套语言包 key 是否与 base 完全一致 | `python3 scripts/check-i18n.py` |
| `check-license-headers.py` | 检查 Java 头为 BSD-3-Clause、资源中无 AGPL、APK 内置许可文本存在 | `python3 scripts/check-license-headers.py` |
| `check-version.py` | 检查 build.gradle、发布清单、脚本示例的版本号是否一致 | `python3 scripts/check-version.py` |

示例：

```bash
./scripts/bump-version.sh patch     # 1.5.5 -> 1.5.6，versionCode +1
./scripts/bump-version.sh minor     # 1.5.5 -> 1.6.0
./scripts/bump-version.sh major     # 1.5.5 -> 2.0.0
```

> 执行前请确认当前分支与工作区干净，`bump-version` 会直接修改 `app/build.gradle`。
> 构建脚本不会自动提交或推送；发布由你手动决定。
