# customer-service-ai

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.x-green.svg)](https://spring.io/projects/spring-ai)
[![Release](https://img.shields.io/github/v/release/andy-library/customer-service-ai?include_prereleases)](https://github.com/andy-library/customer-service-ai/releases)

[English](README.en.md) | **简体中文**

基于 **Spring AI** 的企业级**智能客服编排**应用。

| | |
|---|---|
| **仓库** | https://github.com/andy-library/customer-service-ai |
| **作者 / 维护者** | **andy yang** |
| **许可证** | [Apache License 2.0](LICENSE) |
| **版本** | `0.2.0-rc.1`（准生产 RC） |

本项目是智能客服的**编排层**：负责意图分类、模型路由、知识检索与回答生成。  
**知识沉淀与索引由 Dify 完成**（也可选用本地 PgVector）；本服务调用检索 API 并生成可引用 sources 的回答。

---

## 目录

- [功能特性](#功能特性)
- [架构](#架构)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [API 示例](#api-示例)
- [管理台](#管理台)
- [安全](#安全)
- [文档索引](#文档索引)
- [工程结构](#工程结构)
- [IntelliJ IDEA](#intellij-idea)
- [测试](#测试)
- [贡献](#贡献)
- [许可证](#许可证)

---

## 功能特性

| 领域 | 能力 |
|------|------|
| **路由** | LLM 意图分类 → 选择回答模型 |
| **知识库** | Dify Dataset 检索（主路径）/ 本地 PgVector / 关闭 |
| **模型** | 可插拔 `local`（llama.cpp）或 `cloud`（OpenAI 兼容） |
| **对话** | 同步 JSON + **SSE 流式**（`status` / `delta` / `meta`） |
| **安全** | 可选 API Key、限流、会话归属校验 |
| **质量** | 护栏、依据不足策略、转人工占位 |
| **运维** | 健康检查、Micrometer 指标、审计日志、Docker、Runbook |

---

## 架构

```text
客户端 / BFF
    │  X-API-Key（可选）
    ▼
customer-service-ai
  • 鉴权 / 限流
  • 意图路由
  • 模型网关（local | cloud）
  • 知识检索（Dify | local | none）
  • 会话 / 审计 / 指标
  • 管理台（流式对话测试）
    │
    ├──► OpenAI 兼容 LLM（如 llama.cpp :18080）
    ├──► Dify Dataset API（retrieve）
    └──► PostgreSQL（会话、路由日志、审计）
```

完整说明见 [docs/architecture.md](docs/architecture.md)。

---

## 技术栈

| 组件 | 版本 / 说明 |
|------|-------------|
| OpenJDK | 21 |
| Spring Boot | 3.3.x（由 microservice-framework parent 锁定） |
| Spring AI | 1.1.x |
| 技术底座 | `microservice-framework-starter-parent` |
| 数据库 | PostgreSQL（`knowledge=local` 时使用 PgVector） |
| 知识库 | 默认 Dify Dataset API |

---

## 快速开始

### 环境要求

- OpenJDK **21**、Maven **3.9+**、Docker  
- 可选：[llama.cpp](https://github.com/ggerganov/llama.cpp) `llama-server`、[Dify](https://dify.ai/)

### 1）克隆仓库

```bash
git clone https://github.com/andy-library/customer-service-ai.git
cd customer-service-ai
```

### 2）安装框架 Parent（首次）

```bash
./scripts/install-framework.sh
```

### 3）启动数据库

```bash
docker compose up -d postgres
```

### 4）配置

```bash
cp .env.example .env
# 编辑密钥与端点 — 切勿提交 .env
```

### 5）离线演示（无需外部 LLM）

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mock \
  -Dspring-boot.run.arguments=--server.port=8081
```

### 6）真实模型（本机 llama + Dify 示例）

```bash
# 确保 Chat 模型已启动（OpenAI 兼容），例如 http://127.0.0.1:18080/v1
# 确保 Dify Dataset API 可访问，并在 .env 中填写 DIFY_*
set -a && source .env && set +a
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

| 地址 | 说明 |
|------|------|
| http://localhost:8081/actuator/health | 健康检查 |
| http://localhost:8081/api/v1/ping | 探活 |
| http://localhost:8081/admin | 管理台 |
| http://localhost:8081/admin/chat | **流式**对话测试 |
| http://localhost:8081/swagger-ui.html | OpenAPI |

更多步骤：[docs/getting-started.md](docs/getting-started.md)

---

## 配置说明

| 变量 | 含义 |
|------|------|
| `CS_AI_MODEL_SOURCE` | `local` \| `cloud` |
| `CS_AI_KNOWLEDGE_PROVIDER` | `dify` \| `local` \| `none` |
| `CS_AI_DEFAULT_BASE_URL` | OpenAI 兼容 Base URL（以 `/v1` 结尾） |
| `DIFY_BASE_URL` / `DIFY_API_KEY` / `DIFY_DATASET_ID` | Dify 知识库 API |
| `CSAI_SECURITY_ENABLED` | 是否启用 API Key 鉴权 |
| `CSAI_API_KEY_CLIENT` / `CSAI_API_KEY_ADMIN` | 客户端 / 管理员密钥 |

完整配置：[docs/configuration.md](docs/configuration.md)  
示例文件：[`.env.example`](.env.example)、[`.env.local-llama.example`](.env.local-llama.example)

---

## API 示例

```bash
# 运行时视图（模型源 / 知识库）
curl -s http://localhost:8081/api/v1/runtime | jq

# 同步对话（未开安全时）
curl -s -X POST http://localhost:8081/api/v1/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"如何申请退款？"}' | jq

# 流式对话（SSE）
curl -sN -X POST http://localhost:8081/api/v1/chat/stream \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/event-stream' \
  -d '{"message":"如何申请退款？"}'

# 开启安全后
curl -s -X POST http://localhost:8081/api/v1/chat \
  -H 'Content-Type: application/json' \
  -H "X-API-Key: $CSAI_API_KEY_CLIENT" \
  -d '{"message":"如何申请退款？"}' | jq
```

同步响应 `data` 中常见字段：`answer`、`route`（意图/模型/RAG）、`sources`、`degraded`、`handoff`。

SSE 事件：`status`（阶段进度）、`delta`（增量文本）、`meta`（最终路由与引用）。

---

## 管理台

- 知识库状态 / Dify 检索调试  
- **流式对话测试**（`/admin/chat`）  
- 模型与路由一览（只读）  

---

## 安全

共享或生产环境请务必开启鉴权：

```bash
export CSAI_SECURITY_ENABLED=true
export CSAI_API_KEY_CLIENT="$(openssl rand -hex 24)"
export CSAI_API_KEY_ADMIN="$(openssl rand -hex 24)"
```

- 漏洞请私下报告：[SECURITY.md](SECURITY.md)  
- 运维手册：[docs/operations/RUNBOOK.md](docs/operations/RUNBOOK.md)

---

## 文档索引

| 文档 | 说明 |
|------|------|
| [README.md](README.md) | English README |
| [docs/README.md](docs/README.md) | 文档总索引 |
| [docs/getting-started.md](docs/getting-started.md) | 安装与启动 |
| [docs/architecture.md](docs/architecture.md) | 架构说明 |
| [docs/configuration.md](docs/configuration.md) | 配置参考 |
| [docs/requirements/PRD.md](docs/requirements/PRD.md) | 产品需求 |
| [docs/acceptance/ACCEPTANCE.md](docs/acceptance/ACCEPTANCE.md) | 验收清单 |
| [docs/development/DIFY-AND-MODELS.md](docs/development/DIFY-AND-MODELS.md) | Dify 与模型接入 |
| [docs/development/LOCAL-LLAMA.md](docs/development/LOCAL-LLAMA.md) | 本地 llama.cpp |
| [docs/development/BAILIAN-GLM.md](docs/development/BAILIAN-GLM.md) | 百炼 / 云端 OpenAI 兼容 |
| [docs/development/IDEA-RUN.md](docs/development/IDEA-RUN.md) | IDEA 启动配置 |
| [CHANGELOG.md](CHANGELOG.md) | 版本变更 |
| [CONTRIBUTING.md](CONTRIBUTING.md) | 贡献指南 |

---

## 工程结构

```text
customer-service-ai/
├── src/main/java/com/enterprise/csai/   # 业务模块
├── scripts/                             # 安装、冒烟、本地 LLM 脚本
├── docs/                                # 文档
├── samples/                             # 示例知识文本
├── .run/                                # IDEA 共享运行配置
├── docker-compose.yml
├── Dockerfile
├── README.md                            # 简体中文（默认）
├── README.en.md                         # English
└── pom.xml
```

---

## IntelliJ IDEA

共享运行配置位于 [`.run/`](.run/)：

1. 以 Maven 打开项目，SDK 选 **JDK 21**  
2. `docker compose up -d postgres`  
3. 运行 **Csai · Mock (offline)** 或 **Csai · Local Real (llama + Dify)**  

详细说明：[docs/development/IDEA-RUN.md](docs/development/IDEA-RUN.md)

---

## 测试

```bash
mvn test
```

单元测试不依赖外部大模型。若本机无 Docker，部分集成测试可能被跳过。

---

## 贡献

欢迎贡献代码与文档。请阅读 [CONTRIBUTING.md](CONTRIBUTING.md) 与 [Code of Conduct](CODE_OF_CONDUCT.md)。

维护者：**andy yang**

---

## 许可证

Copyright © 2026 **andy yang**

本项目基于 [Apache License, Version 2.0](LICENSE) 开源。  
第三方声明见 [NOTICE](NOTICE)。
