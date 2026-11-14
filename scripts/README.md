# 脚本目录

存放发版与维护用的小工具，全部本地执行，不依赖外部服务。

| 脚本 | 用途 | 用法 |
|---|---|---|
| `bump-version.sh` | 自动升位 versionCode 与 versionName，避免手改漏一处 | `./scripts/bump-version.sh patch`（或 `minor` / `major`） |

示例：

```bash
./scripts/bump-version.sh patch     # 1.5.3 -> 1.5.4，versionCode +1
./scripts/bump-version.sh minor     # 1.5.3 -> 1.6.0
./scripts/bump-version.sh major     # 1.5.3 -> 2.0.0
```

> 执行前请确认当前分支与工作区干净，脚本会直接修改 `app/build.gradle`。
