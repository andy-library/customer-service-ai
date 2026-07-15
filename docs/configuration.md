# 配置参考

作者：**andy yang**

配置由 Spring Boot 与 `csai.*` 属性驱动。推荐使用环境变量（见 `.env.example`）。

## Profile

| Profile | 用途 |
|---------|------|
| （默认） | 本地 / 真实模型 |
| `mock` | 离线 CI：确定性模型，无外部 LLM |
| `prod` | 强制开启安全；偏保守超时 |

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## 核心环境变量

### 数据库

| 变量 | 默认 | 说明 |
|------|------|------|
| `DB_HOST` | `localhost` | PostgreSQL 主机 |
| `DB_PORT` | `5432` | 端口 |
| `DB_NAME` | `csai` | 库名 |
| `DB_USER` | `csai` | 用户 |
| `DB_PASSWORD` | `csai` | 密码 |

### 模型源

| 变量 | 默认 | 说明 |
|------|------|------|
| `CS_AI_MODEL_SOURCE` | `local` | `local` 或 `cloud` |
| `CS_AI_DEFAULT_BASE_URL` | 本机 llama | OpenAI 兼容 Base（`.../v1`） |
| `CS_AI_DEFAULT_API_KEY` | `sk-local` | Chat 端点密钥 |
| `CS_AI_CLASSIFIER_MODEL` | `local-qwen` | 分类模型名 |
| `CS_AI_ANSWER_STRONG_MODEL` | `local-qwen` | 强回答模型 |
| `CS_AI_ANSWER_FAST_MODEL` | `local-qwen` | 快回答模型 |
| `CS_AI_EMBEDDING_*` | 本机 embed | 仅 `knowledge=local` 时需要 |

### 云端（`CS_AI_MODEL_SOURCE=cloud`）

| 变量 | 说明 |
|------|------|
| `CS_AI_CLOUD_BASE_URL` | 如百炼 `compatible-mode/v1` |
| `CS_AI_CLOUD_API_KEY` | 云端 API Key |
| `CS_AI_CLOUD_*_MODEL` | 分类 / 回答模型名 |

### 知识库（Dify）

| 变量 | 说明 |
|------|------|
| `CS_AI_KNOWLEDGE_PROVIDER` | `dify` / `local` / `none` |
| `DIFY_BASE_URL` | Dataset API 前缀，以 `/v1` 结尾 |
| `DIFY_API_KEY` | Dataset API Key |
| `DIFY_DATASET_ID` | 知识库 UUID |
| `DIFY_SEARCH_METHOD` | 如 `semantic_search` |
| `DIFY_SEGMENT_FALLBACK` | Dify RC 建议 `true` |

### 安全

| 变量 | 默认 | 说明 |
|------|------|------|
| `CSAI_SECURITY_ENABLED` | `false` | 启用 API Key 过滤器 |
| `CSAI_API_KEY_CLIENT` | 开发占位 | 客户端角色 |
| `CSAI_API_KEY_ADMIN` | 开发占位 | 管理员 + 客户端 |
| `CSAI_RATE_LIMIT_ENABLED` | `true` | 限流 |
| `CSAI_RATE_LIMIT_RPM` | `120` | 每主体每分钟请求数 |

> 不要写 `CSAI_API_KEY_CLIENT=`（空串会覆盖默认值）；不需要时请注释掉。

### 韧性与路由

| 变量 | 默认 | 说明 |
|------|------|------|
| `CSAI_CLASSIFIER_TIMEOUT_MS` | `90000` | 分类超时 |
| `CSAI_ANSWER_TIMEOUT_MS` | `180000` | 回答超时 |
| `CSAI_DIFY_TIMEOUT_MS` | `8000` | Dify HTTP 超时 |
| `CSAI_CONFIDENCE_THRESHOLD` | `0.55` | 低于则转人工 |
| `CSAI_HANDOFF_ON_UNKNOWN` | `true` | UNKNOWN 意图转人工 |
| `CSAI_REQUIRE_EVIDENCE` | `true` | RAG 空则「依据不足」 |

## HTTP 请求头

```http
X-API-Key: <client-or-admin-key>
# 或
Authorization: Bearer <client-or-admin-key>
```

## 重要路径

| 路径 | 说明 |
|------|------|
| `GET /actuator/health` | 公开 |
| `GET /api/v1/ping` | 公开 |
| `POST /api/v1/chat` | 开启安全时需鉴权 |
| `POST /api/v1/chat/stream` | SSE 流式 |
| `GET /api/v1/runtime` | 当前模型/知识源视图 |
| `/admin/**` | 开启安全时需 `ADMIN` 角色 |

## 密钥安全

- 切勿提交 `.env`  
- 上线前轮换密钥  
- 云上优先使用密钥管理服务  
