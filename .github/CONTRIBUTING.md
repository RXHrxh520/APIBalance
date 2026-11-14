# 贡献指南

欢迎贡献代码！请按以下步骤操作：
1. Fork 本仓库
2. 创建你的功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的修改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 提交信息规范

为了让提交历史可读，统一使用「类型: 简述」格式，中文描述：

| 类型 | 用在什么时候 |
|---|---|
| `feat` | 新增功能 |
| `fix` | 修复问题 |
| `docs` | 文档、README、文档站 |
| `refactor` | 重构，不改变行为 |
| `style` | 格式、命名、注释 |
| `build` | 构建脚本、Gradle、依赖 |
| `chore` | 杂项（资源、模板、配置） |

示例：

```
feat: 新增火山方舟账户余额查询
fix: 修复欠费余额被四舍五入为 0
docs: 文档拆分为多项目站点结构
build: 升级 targetSdk 至 37
```

一次提交只做一件事；涉及多个不相关改动请拆成多次提交。
