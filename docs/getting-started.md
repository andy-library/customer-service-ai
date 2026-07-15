# 快速开始

作者：**andy yang**

本文说明如何在本机运行 **customer-service-ai**。

## 1. 环境要求

| 工具 | 版本 |
|------|------|
| OpenJDK | 21+ |
| Maven | 3.9+ |
| Docker | 20+（用于 PostgreSQL） |
| 可选 | [llama.cpp](https://github.com/ggerganov/llama.cpp) `llama-server` |
| 可选 | [Dify](https://dify.ai/) 知识库 |

## 2. 克隆仓库

```bash
git clone https://github.com/andy-library/customer-service-ai.git
cd customer-service-ai
```

## 3. 安装框架 Parent（首次）

本项目继承 `microservice-framework-starter-parent`：

```bash
./scripts/install-framework.sh
```

若本地 Maven 仓库已存在该 Parent，可跳过。

## 4. 启动 PostgreSQL

```bash
docker compose up -d postgres
```

## 5. 配置

```bash
cp .env.example .env
# 或本地 llama 默认项：
# cp .env.local-llama.example .env
```

最小配置示例：

```bash
# 数据库
DB_HOST=localhost
DB_PORT=5432
DB_NAME=csai
DB_USER=csai
DB_PASSWORD=csai

# 模型（本机示例）
CS_AI_MODEL_SOURCE=local
CS_AI_DEFAULT_BASE_URL=http://127.0.0.1:18080/v1
CS_AI_DEFAULT_API_KEY=sk-local
CS_AI_CLASSIFIER_MODEL=local-qwen
CS_AI_ANSWER_STRONG_MODEL=local-qwen
CS_AI_ANSWER_FAST_MODEL=local-qwen

# 知识库
CS_AI_KNOWLEDGE_PROVIDER=dify   # 或 local | none
DIFY_BASE_URL=http://127.0.0.1:15001/v1
DIFY_API_KEY=
DIFY_DATASET_ID=

# 安全（共享环境建议开启）
CSAI_SECURITY_ENABLED=false
CSAI_API_KEY_CLIENT=change-me-client
CSAI_API_KEY_ADMIN=change-me-admin
```

校验：

```bash
./scripts/check-env.sh
```

> 注意：环境变量若写成 `KEY=`（空字符串）会覆盖配置默认值；不需要时请**注释掉**该行，不要留空赋值。

## 6. 离线演示（mock，无需 LLM）

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mock \
  -Dspring-boot.run.arguments=--server.port=8081
```

## 7. 真实模型运行

1. 启动 OpenAI 兼容 Chat 服务（可选 Embedding）。  
2. 若 `knowledge.provider=dify`，启动 Dify 并创建知识库 + Dataset API Key。  
3. 填写 `.env` 后：

```bash
set -a && source .env && set +a
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

本机 llama 辅助脚本（可选）：

```bash
export MODEL_DIR="$HOME/LocalModels"
./scripts/local-llm/start-chat.sh
./scripts/local-llm/start-embed.sh   # 仅 knowledge.provider=local 时需要
./scripts/local-llm/healthcheck.sh
```

## 8. 验证

```bash
curl -s http://127.0.0.1:8081/actuator/health | jq
curl -s http://127.0.0.1:8081/api/v1/ping | jq

# 未开安全
curl -s -X POST http://127.0.0.1:8081/api/v1/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"你好"}' | jq

# 已开安全
curl -s -X POST http://127.0.0.1:8081/api/v1/chat \
  -H 'Content-Type: application/json' \
  -H "X-API-Key: $CSAI_API_KEY_CLIENT" \
  -d '{"message":"你好"}' | jq
```

| 地址 | 说明 |
|------|------|
| http://127.0.0.1:8081/admin | 管理台 |
| http://127.0.0.1:8081/admin/chat | **流式**对话测试 |
| http://127.0.0.1:8081/swagger-ui.html | OpenAPI |

## 9. 测试

```bash
mvn test
```

## 下一步

- [architecture.md](architecture.md) — 架构  
- [configuration.md](configuration.md) — 配置详解  
- [operations/RUNBOOK.md](operations/RUNBOOK.md) — 运维  
- [development/IDEA-RUN.md](development/IDEA-RUN.md) — IDEA 启动  
