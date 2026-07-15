# 智能客服系统 MVP — 设计规格

| 属性 | 值 |
|------|-----|
| 文档版本 | 1.0 |
| 日期 | 2026-07-15 |
| 状态 | Approved（设计评审通过，待用户最终确认后进入实现计划） |
| 范围 | MVP 主链路 |

---

## 1. 背景与目标

企业需要一套基于 **Spring AI** 的智能客服系统，实现：

1. **多模型接入**：通过模型网关对接多个 OpenAI-compatible 模型端点。
2. **智能路由**：基于用户问题，使用 LLM 意图分类选择最合适的回答模型。
3. **RAG 知识库**：企业知识沉淀为向量知识库，模型回答必须有据可依并可追溯引用。
4. **可交付 MVP**：需求文档、开发文档、可运行代码、测试与验收清单。

### 1.1 成功标准（MVP）

- 至少配置 **2 个** OpenAI-compatible 模型并可被路由选中。
- 意图分类结果与所选模型在 API 响应中可观测。
- 上传知识文档后，相关问题回答包含 **sources** 引用。
- 知识库无命中时，回答需声明依据不足，避免编造企业事实。
- Docker Compose 可启动；单元/集成测试覆盖路由与 RAG 主路径。
- 交付需求文档、开发文档、验收清单。

### 1.2 非目标（本期不做）

- 微服务拆分部署、Nacos/Gateway 生产集群。
- 多租户、RBAC、SSO、审计合规全链路。
- 工单系统、人机转接、质检评分、话术管理。
- 多模态（图片/语音）、外呼、IM 渠道对接。
- Spring AI 2.x / Spring Boot 4.x。

---

## 2. 约束与技术选型

| 类别 | 选型 | 说明 |
|------|------|------|
| JDK | OpenJDK 21 | 强制 |
| 应用框架 | Spring Boot **3.3.x**（锁定 3.3.13） | 用户要求 3.3 大版本，不用 3.4/3.5/4.x |
| 云组件 BOM | Spring Cloud **2023.0.x** | 与 Boot 3.3 对齐；MVP 仅引入 BOM/约定，不拆服务 |
| AI 框架 | Spring AI **1.1.8** | Boot 3.x 线最新稳定版；**不用** 2.0（绑定 Boot 4） |
| 模型协议 | OpenAI-compatible HTTP API | 统一适配 DeepSeek / 通义 / 智谱 / OpenAI 等 |
| 向量库 | PostgreSQL + **PgVector** | 企业知识与向量同库 |
| 构建 | Maven 3.9+ | 单模块或多模块均可，推荐单模块分包 |
| 界面 | REST + 简易 Admin（Thymeleaf） | 知识管理、对话测试、模型/路由查看 |
| 部署 | Docker Compose | app + postgres(pgvector) |

**版本兼容说明：** Spring AI 1.1 官方文档标注支持 Boot 3.4/3.5。本项目按用户硬约束固定 Boot 3.3.x，在实现阶段通过编译与集成测试验证；若出现不兼容 API，优先在本仓库做兼容适配层，不升级 Boot 大版本。

---

## 3. 架构总览

### 3.1 逻辑架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Admin Web (/admin/**)                      │
│              + REST API (/api/v1/**) + OpenAPI                 │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│                 Modular Monolith (Spring Boot 3.3)            │
│  ┌──────────┐ ┌──────────────┐ ┌────────┐ ┌──────────────┐  │
│  │  chat    │ │ model-gateway│ │ router │ │  knowledge   │  │
│  │ 编排/SSE │ │ 多模型注册表  │ │ LLM意图│ │ 入库/检索/RAG│  │
│  └────┬─────┘ └──────┬───────┘ └───┬────┘ └──────┬───────┘  │
│       │              │             │             │           │
│       └──────────────┴─────────────┴─────────────┘           │
│                         common / config                       │
└───────────────┬─────────────────────────────┬─────────────────┘
                │                             │
       ┌────────▼────────┐         ┌──────────▼──────────┐
       │ PostgreSQL      │         │ OpenAI-compatible   │
       │ + PgVector      │         │ Model Endpoints     │
       └─────────────────┘         └─────────────────────┘
```

### 3.2 主链路（问答）

```
用户问题
  → (可选) 加载 session 历史
  → Router: classifier 模型做意图分类 → Intent + confidence
  → ModelRegistry: 按意图映射 answer 模型
  → Knowledge: 若意图需要知识 或 配置强制 RAG → top-k 检索
  → Chat: 组装 system/user prompt（含上下文片段 + 引用约束）
  → answer 模型生成
  → 持久化 message + route_log
  → 返回 answer + route + sources
```

### 3.3 知识入库链路

```
上传 PDF/MD/TXT
  → 解析文本
  → 切片 (chunk size / overlap 可配)
  → Embedding 模型向量化
  → 写入 PgVector + 文档元数据
  → 状态: PENDING → INDEXED | FAILED
```

---

## 4. 模块设计

包根：`com.enterprise.csai`

### 4.1 `modelgateway` — 模型网关

**职责**

- 从配置加载多个模型定义（id、baseUrl、apiKey、modelName、角色、超时）。
- 为每个模型创建 `ChatModel`（OpenAI-compatible）。
- 提供统一调用入口：`ModelGateway.chat(modelId, messages, options)`。
- Embedding 使用独立配置的 embedding 模型（可与 chat 同厂商不同 model）。
- 错误归一：`ModelInvocationException`（超时、4xx/5xx、限流）。

**关键类型**

```text
ModelDefinition { id, displayName, baseUrl, apiKey, modelName,
                  role: CLASSIFIER | ANSWER | EMBEDDING, enabled, timeoutMs }
ModelRegistry { getChatModel(id), listModels(), getDefault(role) }
ModelGateway { chat(...), stream(...) }
```

**设计决策**

- 不使用独立进程的网关服务；进程内网关 + 配置即「模型网关」。
- 同一 OpenAI starter 通过 **多 baseUrl / 多 ChatModel Bean** 实现多厂商。
- 后续拆服务时，将 `ModelGateway` 接口保留，实现改为 HTTP 客户端即可。

### 4.2 `router` — 智能路由

**职责**

- 调用 classifier 角色模型，将用户问题分类为有限意图枚举。
- 根据 `intent → answerModelId` 映射选择回答模型。
- 决定是否启用 RAG（意图标记 `requiresKnowledge` 或全局 `forceRag`）。
- 输出 `RoutingDecision` 供响应与 `route_log` 使用。

**意图枚举（MVP 固定，可配置扩展标签描述）**

| Intent | 说明 | 默认 requiresKnowledge |
|--------|------|------------------------|
| PRODUCT | 产品功能/使用 | true |
| BILLING | 账单/套餐/支付 | true |
| TECH_SUPPORT | 技术故障/对接 | true |
| POLICY | 政策/条款/流程 | true |
| CHITCHAT | 闲聊/问候 | false |
| UNKNOWN | 无法判断 | true（偏保守，走知识库） |

**分类 Prompt 要点**

- 仅输出 JSON：`{"intent":"...","confidence":0.0-1.0,"reason":"..."}`。
- 解析失败 → `UNKNOWN` + 默认 answer 模型。
- temperature 低（如 0），提高稳定性。

### 4.3 `knowledge` — RAG 知识库

**职责**

- 文档上传、列表、删除（级联删除向量块）。
- 文本提取：TXT、Markdown；PDF 使用可用 PDF 库（如 pdfbox）。
- `TokenTextSplitter` 或等价切片策略。
- `EmbeddingModel` + `VectorStore`（PgVector）。
- 检索：`similaritySearch(query, topK, threshold)`。
- 返回 `List<KnowledgeChunk>`（content、documentId、title、score）。

**回答约束（注入 system prompt）**

- 优先依据检索片段回答；片段不足时明确说明「知识库暂无足够依据」。
- 不得编造工单号、价格、内部政策等未出现在片段中的事实。
- 响应中附带 sources（文档名 + 片段摘要）。

### 4.4 `chat` — 对话编排

**职责**

- REST：同步问答、SSE 流式问答。
- 会话：`sessionId` 可选；无则新建。MVP **支持最近 N 轮** 作为上下文（默认 N=6 条消息）。
- 编排：Router → Knowledge → Gateway → 组装响应。
- 持久化：session、message、route_log。

**响应结构（示意）**

```json
{
  "sessionId": "uuid",
  "answer": "...",
  "route": {
    "intent": "PRODUCT",
    "confidence": 0.92,
    "classifierModelId": "qwen-turbo",
    "answerModelId": "deepseek-chat",
    "ragEnabled": true
  },
  "sources": [
    { "documentId": "...", "title": "产品手册.pdf", "snippet": "...", "score": 0.81 }
  ]
}
```

### 4.5 `admin` — 简易管理后台

**页面（Thymeleaf + 少量原生 JS）**

1. 知识库：上传、列表、删除、索引状态。
2. 对话测试台：提问、展示 answer / route / sources。
3. 模型与路由：只读展示当前配置的模型与意图映射。

不做登录鉴权（MVP）；文档中标注生产需加鉴权。

### 4.6 `common`

- 统一错误体、业务异常。
- 配置属性绑定：`CsaiProperties`。
- 观测：结构化日志（sessionId、intent、modelId、latencyMs）。
- OpenAPI（springdoc）。

---

## 5. 数据模型

### 5.1 表结构

**cs_document**

| 列 | 类型 | 说明 |
|----|------|------|
| id | UUID PK | |
| title | varchar | 展示名 |
| filename | varchar | 原始文件名 |
| content_type | varchar | MIME |
| status | varchar | PENDING/INDEXED/FAILED |
| chunk_count | int | |
| error_message | text | 可空 |
| created_at / updated_at | timestamptz | |

**cs_document_chunk**

| 列 | 类型 | 说明 |
|----|------|------|
| id | UUID PK | |
| document_id | UUID FK | |
| chunk_index | int | |
| content | text | |
| metadata | jsonb | 可选 |
| embedding | vector(dim) | PgVector；维度与 embedding 模型一致 |

> 实现时优先使用 Spring AI `PgVectorStore` 的表结构约定；业务 `cs_document` 与 store 元数据通过 documentId 关联。若 store 自带表名不同，在开发文档中映射说明。

**cs_chat_session**

| 列 | 类型 |
|----|------|
| id | UUID PK |
| title | varchar 可空 |
| created_at / updated_at | timestamptz |

**cs_chat_message**

| 列 | 类型 |
|----|------|
| id | UUID PK |
| session_id | UUID FK |
| role | USER/ASSISTANT/SYSTEM |
| content | text |
| created_at | timestamptz |

**cs_route_log**

| 列 | 类型 |
|----|------|
| id | UUID PK |
| session_id | UUID 可空 |
| message_id | UUID 可空 |
| user_query | text |
| intent | varchar |
| confidence | double |
| classifier_model_id | varchar |
| answer_model_id | varchar |
| rag_enabled | boolean |
| latency_ms | bigint |
| created_at | timestamptz |

### 5.2 配置模型（application.yml）

```yaml
csai:
  models:
    - id: classifier-default
      base-url: ${CS_AI_CLASSIFIER_BASE_URL}
      api-key: ${CS_AI_CLASSIFIER_API_KEY}
      model-name: ${CS_AI_CLASSIFIER_MODEL:gpt-4o-mini}
      role: CLASSIFIER
    - id: answer-strong
      base-url: ${CS_AI_ANSWER_STRONG_BASE_URL}
      api-key: ${CS_AI_ANSWER_STRONG_API_KEY}
      model-name: ${CS_AI_ANSWER_STRONG_MODEL}
      role: ANSWER
    - id: answer-fast
      base-url: ${CS_AI_ANSWER_FAST_BASE_URL}
      api-key: ${CS_AI_ANSWER_FAST_API_KEY}
      model-name: ${CS_AI_ANSWER_FAST_MODEL}
      role: ANSWER
  embedding:
    base-url: ${CS_AI_EMBEDDING_BASE_URL}
    api-key: ${CS_AI_EMBEDDING_API_KEY}
    model-name: ${CS_AI_EMBEDDING_MODEL:text-embedding-3-small}
    dimensions: 1536
  router:
    classifier-model-id: classifier-default
    default-answer-model-id: answer-fast
    force-rag: false
    intent-model-mapping:
      PRODUCT: answer-strong
      BILLING: answer-strong
      TECH_SUPPORT: answer-strong
      POLICY: answer-strong
      CHITCHAT: answer-fast
      UNKNOWN: answer-fast
  rag:
    top-k: 5
    similarity-threshold: 0.5
    chunk-size: 800
    chunk-overlap: 100
  chat:
    history-max-messages: 6
```

---

## 6. API 设计

基路径：`/api/v1`  
Content-Type：`application/json`（上传为 `multipart/form-data`）

### 6.1 对话

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/chat` | 同步问答 |
| POST | `/chat/stream` | SSE 流式；最终可带 route/sources 元事件或尾包 |

**请求**

```json
{
  "sessionId": "optional-uuid",
  "message": "如何退款？",
  "options": {
    "forceRag": null,
    "overrideAnswerModelId": null
  }
}
```

### 6.2 知识库

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/knowledge/documents` | multipart: file, title? |
| GET | `/knowledge/documents` | 列表 |
| GET | `/knowledge/documents/{id}` | 详情 |
| DELETE | `/knowledge/documents/{id}` | 删除文档及向量 |
| POST | `/knowledge/search` | 调试检索：`{ "query": "...", "topK": 5 }` |

### 6.3 模型与路由

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/models` | 已注册模型（脱敏，不含 apiKey） |
| GET | `/router/intents` | 意图列表与映射 |
| POST | `/router/classify` | 仅分类，调试用 |

### 6.4 管理页

| 路径 | 说明 |
|------|------|
| `/admin` | 首页 |
| `/admin/knowledge` | 知识库 |
| `/admin/chat` | 测试台 |
| `/admin/models` | 模型与路由只读 |

### 6.5 错误体

```json
{
  "timestamp": "2026-07-15T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "message must not be blank",
  "path": "/api/v1/chat"
}
```

---

## 7. 错误处理与韧性

| 场景 | 处理 |
|------|------|
| 分类模型失败 | 降级 UNKNOWN + default answer 模型；记录日志 |
| 回答模型失败 | 若配置了 fallback 模型则切换一次；否则 502 + 明确错误 |
| Embedding/入库失败 | 文档 status=FAILED，error_message 记录原因 |
| 检索无结果 | ragEnabled=true 时仍调用模型，但 prompt 要求声明无依据 |
| 超时 | 每模型 timeoutMs；全局 read timeout |

MVP **不做** 分布式熔断器；可后续接入 Resilience4j（Spring Cloud 生态）。

---

## 8. 安全（MVP 基线）

- API Key **仅**通过环境变量注入，禁止提交真实密钥。
- 管理端与 API **暂无鉴权**（本地/内网演示）；验收文档标注生产必加。
- 上传文件：限制扩展名（pdf/md/txt）与大小（默认 10MB）。
- 日志脱敏：不打印 apiKey、完整用户隐私（可选截断）。

---

## 9. 可观测性

- 每次问答日志字段：`sessionId, intent, answerModelId, ragEnabled, sourceCount, latencyMs`。
- Actuator：`/actuator/health`（含 DB）。
- route_log 表支持事后分析路由分布。

---

## 10. 测试策略

| 层级 | 内容 |
|------|------|
| 单元 | 意图 JSON 解析、映射、切片参数、降级逻辑（Mock ChatModel） |
| 集成 | Testcontainers PostgreSQL(pgvector)；上传 → 检索；Chat 编排 Mock 模型 |
| 手工验收 | 按验收清单：双模型路由、RAG 引用、无知识声明 |
| 契约 | OpenAPI 生成；示例 curl 写入开发文档 |

无真实外网 Key 时：提供 `MockChatModel` / WireMock 配置 profile `mock`，保证 CI 可绿。

---

## 11. 交付物清单

1. `docs/requirements/PRD-智能客服-MVP.md` — 需求文档  
2. 本文 — 设计规格  
3. `docs/development/DEV-智能客服-MVP.md` — 开发与运行文档  
4. `docs/acceptance/ACCEPTANCE-智能客服-MVP.md` — 验收清单  
5. 可运行源码 + `docker-compose.yml` + `.env.example`  
6. 自动化测试  

---

## 12. Key Decisions

| 决策 | 选择 | 理由 |
|------|------|------|
| 交付范围 | MVP 主链路 | 控制范围，先打通价值闭环 |
| 服务形态 | 模块化单体 | 用户选择；后续可拆 |
| 模型协议 | OpenAI-compatible only | 覆盖主流国产/海外模型，实现简单 |
| 向量库 | PgVector | 企业知识与业务数据一体，Spring AI 支持成熟 |
| 路由 | LLM 意图分类 | 用户要求；比纯规则更灵活 |
| 框架版本 | Boot 3.3 + AI 1.1.8 | 满足 3.3 约束，且 AI 为 1.x 最新稳定 |
| 前端 | Thymeleaf 管理台 | 足够验收，避免引入重前端栈 |
| 鉴权 | MVP 不加 | 加速交付；文档标明风险 |

---

## 13. Open Questions

无阻塞性问题。以下为**实现期默认**，用户可随时覆盖：

1. Embedding 维度默认 **1536**（text-embedding-3-small）；若使用其他模型需同步改 `dimensions` 与向量列。  
2. 流式 SSE 协议：文本 delta 事件 + 结束时 JSON 元数据事件。  
3. Spring Cloud 仅 BOM，不启用 Config/Gateway。

---

## 14. PR Plan

### PR-1: 工程骨架与基础设施

- **内容**：Maven 工程、Boot 3.3.13、Spring AI BOM 1.1.8、包结构、application.yml、Docker Compose（pgvector）、Actuator、OpenAPI、`.env.example`  
- **依赖**：无  
- **验收**：应用可启动（可先无真实模型，使用 mock profile）

### PR-2: 模型网关

- **内容**：`ModelDefinition`、`ModelRegistry`、多 ChatModel 创建、统一调用与异常、模型列表 API  
- **依赖**：PR-1  
- **验收**：配置 2 个模型后 `GET /models` 可见；单元测试覆盖注册逻辑

### PR-3: 智能路由

- **内容**：意图枚举、分类 Prompt、JSON 解析、意图→模型映射、`POST /router/classify`、route_log 实体  
- **依赖**：PR-2  
- **验收**：Mock 分类返回稳定映射到配置模型

### PR-4: 知识库 RAG

- **内容**：文档上传/列表/删除、切片、Embedding、PgVector、检索 API、引用 DTO  
- **依赖**：PR-1（DB）、PR-2（embedding）  
- **验收**：上传样例文档后 search 返回相关片段

### PR-5: 对话编排

- **内容**：`POST /chat`、SSE `/chat/stream`、session/message 持久化、完整编排与降级  
- **依赖**：PR-2、PR-3、PR-4  
- **验收**：端到端问答返回 answer+route+sources

### PR-6: 管理后台与文档验收

- **内容**：Thymeleaf 三页、README、需求/开发/验收文档定稿、集成测试、验收走查  
- **依赖**：PR-5  
- **验收**：按验收清单全部勾选

---

## 15. 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| Boot 3.3 与 Spring AI 1.1 官方支持矩阵不完全一致 | 编译/自动配置问题 | 实现期验证；必要时锁定兼容依赖或薄适配 |
| 各厂商 OpenAI 兼容差异 | 个别参数不支持 | 仅用通用 chat/embeddings 字段；可配 path |
| Embedding 维度不一致 | 向量写入失败 | 配置 dimensions；文档强调同一模型 |
| 分类不稳定 | 路由抖动 | temperature=0、枚举约束、UNKNOWN 降级 |
| 大 PDF 解析慢 | 上传超时 | 限制大小；异步索引（MVP 可同步，超时调大） |

---

*本文档为 MVP 实现的唯一设计真源。实现计划以本文 PR Plan 展开。*
