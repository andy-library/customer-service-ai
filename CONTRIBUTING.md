# 贡献指南

感谢关注 **customer-service-ai**。

维护者：**andy yang**

> English version: [CONTRIBUTING.en.md](CONTRIBUTING.en.md)

## 如何贡献

- 通过 GitHub Issues 报告缺陷、提出需求  
- 改进文档与示例  
- 提交聚焦的 Pull Request（修 bug / 小范围特性）  
- 分享不同模型后端（llama.cpp、云厂商、Dify）的部署经验  

## 开发环境

1. **依赖**  
   - OpenJDK 21+  
   - Maven 3.9+  
   - Docker（PostgreSQL）  
   - 可选：本机 llama.cpp、Dify  

2. **安装框架 Parent（首次）**

   ```bash
   ./scripts/install-framework.sh
   ```

3. **测试**

   ```bash
   mvn test
   ```

4. **运行**

   ```bash
   docker compose up -d postgres
   cp .env.example .env
   set -a && source .env && set +a
   mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
   ```

详见 [docs/getting-started.md](docs/getting-started.md)。

## 编码约定

- 与 `microservice-framework` 锁定的 Boot 版本保持一致，勿随意升级 Boot  
- 外部系统（模型、知识库）优先通过端口/适配器接入  
- 禁止提交密钥（`.env`、API Key、Token）  
- 行为变更请补充/更新单元测试  
- 尽量保持 `/api/v1` 兼容；破坏性变更写入 `CHANGELOG.md`  

## Pull Request（推荐流程）

**外部贡献必须走 PR**；维护者可直接推送到 `main`（见下方说明）。

1. Fork 仓库，从最新 `main` 创建功能分支（建议：`feat/...`、`fix/...`、`docs/...`）  
2. 提交信息清晰、改动聚焦；一个 PR 只做一件事  
3. 本地验证：

   ```bash
   ./scripts/install-framework.sh
   mvn test
   bash ./scripts/ci-secret-scan.sh
   ```

4. 打开 PR，填写模板（摘要 / 验证方式 / 检查清单）  
5. 等待 GitHub Actions **全部通过**：
   - `test`：安装 framework + `mvn test`
   - `secrets-hygiene`：防止把真实密钥推入仓库  
6. 维护者审核后合并（优先 Squash merge）；合并后功能分支可删除  

### 维护者说明（单人维护）

- 仓库开启了 `main` 分支规则：对**非维护者**要求通过 PR，且 CI 通过后再合并。  
- **管理员（维护者）拥有 bypass**，可在紧急修复、文档微调等场景下直接推送到 `main`，避免自我卡死。  
- 即便直推，`push` 仍会触发 CI；若 CI 失败请尽快修复，保持主干可构建。  

## Issue 报告

请尽量包含：

- 版本 / commit  
- 运行环境（本机 / Docker / K8s）  
- 模型源（local/cloud）与知识提供方（dify/local/none）  
- 复现步骤  
- 日志（务必脱敏）  

## 安全问题

**不要**公开提 Issue。见 [SECURITY.md](SECURITY.md)。

## 许可证

贡献代码即表示同意以 **Apache License 2.0** 授权。
