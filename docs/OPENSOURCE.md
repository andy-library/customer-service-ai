# 开源说明

本仓库面向 GitHub 公开托管。

| 项 | 值 |
|----|-----|
| 作者 / 版权 | **andy yang** |
| 许可证 | Apache License 2.0（见 [LICENSE](../LICENSE)、[NOTICE](../NOTICE)） |
| 默认文档语言 | **简体中文**（[README.md](../README.md)） |
| 英文 README | [README.en.md](../README.en.md) |

## 发布内容

- `src/` 应用源码  
- `scripts/` 脚本（不含密钥）  
- `docs/` 中文文档  
- `samples/` 示例知识文本  
- Docker / Compose  
- 社区文件：CONTRIBUTING、CODE_OF_CONDUCT、SECURITY、CHANGELOG  

## 禁止发布

- `.env`（已 gitignore）  
- 真实 API Key、Dataset Token、云凭证  
- 模型权重（`*.gguf`）  
- 私有基础设施主机名与密码  

## 维护者身份

开源署名统一为：

```text
andy yang
```

推荐本仓库本地 Git 配置：

```bash
git config user.name "andy yang"
git config user.email "YOUR_PUBLIC_GITHUB_NOREPLY@users.noreply.github.com"
```

## 建议的 GitHub 设置

1. 公开仓库 `customer-service-ai`  
2. 开启 Issues（可选 Discussions）  
3. 开启 Private vulnerability reporting + Dependabot  
4. 保护 `main`（Ruleset）：外部改动走 PR + CI；**管理员可 bypass 直推**，避免单人维护卡死  
5. CI：`test`（install-framework + `mvn test`）与 `secrets-hygiene`  
6. Topics：`spring-ai`、`java`、`dify`、`rag`、`customer-service`、`openai-compatible`  
7. 描述示例：  

   > 基于 Spring AI 的企业智能客服编排：可插拔本地/云端大模型，Dify 知识检索。

## 仓库地址（已发布）

```text
https://github.com/andy-library/customer-service-ai
```

## 社区阅读路径

1. [README.md](../README.md)（中文主页）  
2. [getting-started.md](getting-started.md)  
3. [architecture.md](architecture.md)  
4. [configuration.md](configuration.md)  
5. [operations/RUNBOOK.md](operations/RUNBOOK.md)  
6. [requirements/PRD.md](requirements/PRD.md)  

## 依赖说明

运行时 Parent `microservice-framework-starter-parent` 可能仅在组织内可解析。开源使用者可：

- 从已发布的 Maven 坐标安装；或  
- 执行 `scripts/install-framework.sh`（若源码可得）；或  
- Fork 后改为公共 Spring Boot BOM（欢迎贡献）  

请在仓库说明中写清你们的解析方式。
