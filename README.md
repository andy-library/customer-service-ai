# Customer Service AI (0.2.0-rc.1 准生产)

企业智能客服：基于 [microservice-framework](https://github.com/andy-library/microservice-framework) + Spring AI。

**知识库由 Dify 构建；本应用负责编排、意图路由、多模型网关与检索调用。**  
**模型支持 local（llama.cpp）/ cloud（百炼等 OpenAI 兼容）切换。**  
**准生产能力：API Key 鉴权、限流、超时、降级标记、转人工占位、审计与 Metrics。**

## 技术栈

| 组件 | 版本 / 说明 |
|------|-------------|
| OpenJDK | 21 |
| microservice-framework | 1.0.0-alpha.1（Boot 3.3.13 / Cloud 2023.0.6） |
| Spring AI | 1.1.8 |
| 知识库 | **Dify** Dataset Retrieve API（主路径） |
| 本地模型 | llama.cpp OpenAI 兼容：Chat `:18080` / Embed `:18081` |
| 云端模型 | 可插拔 OpenAI 兼容（默认百炼 `compatible-mode`） |
| DB | PostgreSQL（会话/路由日志；local 知识库时 + PgVector） |

## 架构

```
用户 ──► Spring AI（路由 / 会话 / 编排）
              ├─ CS_AI_MODEL_SOURCE=local  → llama :18080 (local-qwen)
              ├─ CS_AI_MODEL_SOURCE=cloud  → 百炼等 (csai.cloud.*)
              └─ CS_AI_KNOWLEDGE_PROVIDER=dify → Dify Dataset retrieve
```

## 快速开始（推荐：本机 llama + Dify）

### 0. 本机已启动的两个 llama.cpp

| 服务 | 端口 | model id | 用途 |
|------|------|----------|------|
| Chat | **18080** | `local-qwen` | 分类 + 回答 |
| Embedding | **18081** | `local-bge-m3` | 仅 `knowledge=local` 时需要 |

```bash
./scripts/local-llm/healthcheck.sh
```

### 1. 安装技术底座（首次）

```bash
./scripts/install-framework.sh
```

### 2. 数据库

```bash
docker compose up -d postgres
```

### 3. 配置

```bash
cp .env.local-llama.example .env
# 填写：
#   DIFY_BASE_URL / DIFY_API_KEY / DIFY_DATASET_ID
#   CS_AI_MODEL_SOURCE=local
#   CS_AI_KNOWLEDGE_PROVIDER=dify
./scripts/check-env.sh
```

### 4. 启动应用

```bash
set -a && source .env && set +a
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### 5. 访问

| 地址 | 说明 |
|------|------|
| http://localhost:8081/admin | 管理后台 |
| http://localhost:8081/admin/knowledge | Dify 检索调试 |
| http://localhost:8081/admin/chat | 对话测试 |
| http://localhost:8081/api/v1/runtime | 当前模型源 / 知识库 |
| http://localhost:8081/api/v1/ping | 探活 |
| http://localhost:8081/api/v1/chat | 同步问答 |
| http://localhost:8081/swagger-ui.html | OpenAPI |

### 切换云端模型

```bash
# .env
CS_AI_MODEL_SOURCE=cloud
CS_AI_CLOUD_API_KEY=sk-xxx
# 重启应用即可（Registry 启动时按 model-source 加载）
```

### 生产安全开关

```bash
export CSAI_SECURITY_ENABLED=true
export CSAI_API_KEY_CLIENT=your-client-key
export CSAI_API_KEY_ADMIN=your-admin-key
# 请求头：X-API-Key: your-client-key
# 或：Authorization: Bearer your-client-key
```

运维手册：`docs/operations/RUNBOOK.md`  
生产 PRD/设计：`docs/requirements/PRD-智能客服-Production-v1.md`

详见：`docs/development/DIFY-知识库与可插拔模型.md`、`docs/development/BAILIAN-GLM-配置.md`

### 无密钥演示（mock）

```bash
docker compose up -d postgres
mvn spring-boot:run -Dspring-boot.run.profiles=mock \
  -Dspring-boot.run.arguments=--server.port=8081
```

## API 示例

```bash
# 当前运行时配置（无密钥）
curl -s http://localhost:8081/api/v1/runtime | jq

# 对话
curl -s -X POST http://localhost:8081/api/v1/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"如何申请退款？"}' | jq

# 知识检索（走 Dify 或 local）
curl -s -X POST http://localhost:8081/api/v1/knowledge/search \
  -H 'Content-Type: application/json' \
  -d '{"query":"退款政策","topK":5}' | jq
```

> `provider=dify` 时不要用本服务上传文档；请在 **Dify 控制台** 管理知识库。

## 测试

```bash
mvn test
```

## 文档索引

| 文档 | 路径 |
|------|------|
| **进度记忆** | `docs/superpowers/plans/PROGRESS.md` |
| **Dify + 可插拔模型** | `docs/development/DIFY-知识库与可插拔模型.md` |
| 本地 llama 部署 | `docs/development/LOCAL-LLAMA-全量部署-M1.md` |
| 百炼 glm | `docs/development/BAILIAN-GLM-配置.md` |
| 开发文档 | `docs/development/DEV-智能客服-MVP.md` |
| 设计 / PRD / 验收 | `docs/superpowers/` · `docs/requirements/` · `docs/acceptance/` |

## 模块结构

```
com.enterprise.csai
  common / modelgateway / router / knowledge / chat / admin
```

主链路：提问 → LLM 意图分类 → 选模型 →（Dify/local RAG）→ 回答 + sources。
