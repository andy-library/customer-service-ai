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

## Pull Request

1. Fork 并创建功能分支  
2. 提交信息清晰、改动聚焦  
3. 确保 `mvn test` 通过  
4. PR 说明：改了什么、为何改、如何验证、关联 Issue  

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
