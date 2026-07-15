# 智能客服系统 MVP — 设计规格

| 属性 | 值 |
|------|-----|
| 文档版本 | **1.1** |
| 日期 | 2026-07-15 |
| 状态 | 已按技术底座修订（待用户确认后进入实现计划） |
| 范围 | MVP 主链路 |
| 技术底座 | [andy-library/microservice-framework](https://github.com/andy-library/microservice-framework) **1.0.0-alpha.1** |
| 底座分析 | `docs/analysis/microservice-framework-foundation-analysis.md` |

### 修订记录

| 版本 | 说明 |
|------|------|
| 1.0 | 初版：模块化单体 + Spring AI + PgVector |
| 1.1 | **强制接入 microservice-framework**；统一 ApiResponse/日志/观测/DB/安全；PR-0 框架接入 |

---

## 1. 背景与目标

企业需要一套基于 **Spring AI** 的智能客服系统，并建立在企业内部 **microservice-framework** 技术底座之上，实现：

1. **工程标准化**：继承 Framework Parent/BOM/Starter，复用 Web、日志、可观测、数据库、安全等能力。  
2. **多模型接入**：进程内模型网关对接多个 OpenAI-compatible 端点。  
3. **智能路由**：LLM 意图分类选择回答模型。  
4. **RAG 知识库**：PgVector 企业知识沉淀，回答可追溯 sources。  
5. **可交付 MVP**：需求/设计/开发/验收文档 + 可运行代码与测试。

### 1.1 成功标准（MVP）

- 应用 **继承** `microservice-framework-starter-parent:1.0.0-alpha.1`，并启用基础 Starter 链路。  
- 至少配置 **2 个** OpenAI-compatible 模型并可被路由选中。  
- 意图与所选模型在 `ApiResponse.data.route` 中可观测；带 `requestId`。  
- 上传知识后相关问答返回 **sources**。  
- 无相关知识时声明依据不足。  
- Docker Compose 可启动；自动化测试覆盖路由与 RAG 主路径。  
- 交付文档齐全。

### 1.2 非目标（本期不做）

- 微服务拆分、Nacos/Apollo 配置中心集群、外网 API Gateway 部署。  
- 完整 JWT/SSO/RBAC 生产鉴权（仅预留 security-starter 与路径策略）。  
- 工单、转人工、质检、多租户、多模态渠道。  
- 将 AI 能力回灌为框架官方 `ai-starter`（可列二期）。  
- Spring Boot 4 / Spring AI 2.x。

---

## 2. 约束与技术选型

| 类别 | 选型 | 说明 |
|------|------|------|
| 技术底座 | **microservice-framework 1.0.0-alpha.1** | 强制；先 `mvn install` Parent 再构建业务 |
| JDK | OpenJDK 21 | 与框架 `java.version=21` 一致 |
| Spring Boot | **3.3.13** | 框架保护版本，**禁止**业务覆盖 |
| Spring Cloud | **2023.0.6** | 由框架 Dependencies BOM 纳管 |
| 业务 Parent | `microservice-framework-starter-parent` | 唯一应用入口 Parent |
| AI 框架 | Spring AI **1.1.8** | 应用侧 import `spring-ai-bom`（框架未纳管） |
| 模型协议 | OpenAI-compatible | 多 baseUrl / 多 ChatModel |
| 业务库 + 向量 | PostgreSQL + **PgVector** | database-starter 支持 postgres JDBC |
| 迁移 | Flyway | `framework.database.migration` |
| Web 契约 | `ApiResponse` | web-starter 自动包装 |
| 界面 | REST + Thymeleaf Admin | `/admin/**` |
| 部署 | Docker Compose | app + postgres(pgvector)（可选 redis/minio） |

**版本兼容说明：** Spring AI 1.1 官方标注偏 Boot 3.4/3.5；本项目与框架锁定 Boot 3.3.13，实现期验证。禁止为 AI 升级 Boot。

---

## 3. 架构总览

### 3.1 逻辑架构

```
┌──────────────────────────────────────────────────────────────┐
│           Admin (/admin/**) + REST (/api/v1/**) + OpenAPI      │
└───────────────────────────────┬──────────────────────────────┘
                                │ ApiResponse / RequestId
┌───────────────────────────────▼──────────────────────────────┐
│         customer-service-ai（模块化单体业务应用）                 │
│  parent: microservice-framework-starter-parent:1.0.0-alpha.1   │
│ ┌────────────────────────────────────────────────────────────┐ │
│ │ Framework Starters                                           │ │
│ │ common · json · web · logging · observability                │ │
│ │ database · async · security  (± redis · object-storage)      │ │
│ └────────────────────────────────────────────────────────────┘ │
│ ┌─────────┐ ┌──────────────┐ ┌────────┐ ┌───────────┐ ┌──────┐ │
│ │  chat   │ │ model-gateway│ │ router │ │ knowledge │ │admin │ │
│ └─────────┘ └──────────────┘ └────────┘ └───────────┘ └──────┘ │
│                    Spring AI 1.1.8（应用侧依赖）                  │
└──────────────┬───────────────────────────────┬─────────────────┘
               │                               │
      PostgreSQL + PgVector            OpenAI-compatible
      (Flyway 业务表 + 向量)            多模型端点
```

### 3.2 主链路（问答）

```
请求进入 web-starter（RequestId）
  → ChatOrchestrator
  → Router（CLASSIFIER 模型 → Intent）
  → ModelRegistry 选 ANSWER 模型
  → Knowledge 检索（按意图/forceRag）
  → Gateway 调用回答模型
  → 持久化 message + route_log（database-starter / JDBC）
  → 返回 DTO → ApiResponse 包装
```

### 3.3 知识入库

```
上传 → 校验类型/大小 → 解析 → 切片 → Embedding → PgVector
     → cs_document 状态 INDEXED | FAILED
```

---

## 4. 框架接入规范

### 4.1 必选 Starter

| Starter | 配置前缀 | MVP 用途 |
|---------|----------|----------|
| common | `framework.common` | IdGenerator、异常基础、分页 |
| json | `framework.json` | Jackson 统一 |
| web | `framework.web` | ApiResponse、RequestId、异常处理 |
| logging | `framework.logging` | 结构化日志与脱敏 |
| observability | `framework.observability` | health/metrics |
| database | `framework.database` | 连接池默认值、Flyway、事务 |
| async | `framework.async` | 受治理线程池 |
| security | `framework.security` | 路径安全；MVP permit 演示路径 |

### 4.2 可选 Starter（预留）

- `redis-starter`：限流/缓存  
- `object-storage-starter`：知识原文对象存储  
- 其余（kafka/feign/nacos/apollo/es/xxl-job/audit/drools…）二期  

### 4.3 响应与错误

成功（自动或显式）：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "sessionId": "...",
    "answer": "...",
    "route": { },
    "sources": [ ]
  },
  "timestamp": 1710000000000,
  "requestId": "..."
}
```

- 业务异常：`BusinessException`（web-starter）或项目内继承框架错误体系的领域异常。  
- **禁止**再定义第二套全局错误 JSON 结构。  
- SSE：验证 `SseEmitter` 不被错误包装；必要时排除自动包装或使用专用端点策略。

### 4.4 安全（MVP）

```yaml
framework:
  security:
    path:
      permit-paths:
        - /api/v1/**
        - /admin/**
        - /swagger-ui/**
        - /v3/api-docs/**
        - /actuator/health
        - /actuator/info
```

生产环境必须收紧（非 MVP 编码范围，验收文档提示）。

### 4.5 配置分层

| 前缀 | 归属 |
|------|------|
| `framework.*` | 技术底座 |
| `spring.*` | Boot / 数据源 / AI 部分自动配置 |
| `csai.*` | 客服业务：模型列表、路由映射、RAG 参数 |

### 4.6 底座安装

```bash
git clone https://github.com/andy-library/microservice-framework.git
cd microservice-framework/SourceCode/microservice-framework-parent
mvn clean install -DskipTests   # 或完整 verify
```

业务仓库提供 `scripts/install-framework.sh` 与 README 说明。CI 需先 install 再 build 业务（在 alpha 未发布 Central 前）。

---

## 5. 模块设计

包根：`com.enterprise.csai`  
**不** 使用 `com.microservice.framework` 作为业务包名。

### 5.1 `modelgateway`

- 配置驱动多模型：`csai.models[]`（id、baseUrl、apiKey、modelName、role、enabled、timeoutMs）。  
- 构建多个 OpenAI-compatible `ChatModel`，注册到 `ModelRegistry`。  
- `ModelGateway.chat / stream`；异常归一为业务可映射错误（超时、鉴权失败、上游 5xx）。  
- Embedding 独立配置块 `csai.embedding`。

### 5.2 `router`

- CLASSIFIER 模型 JSON 分类。  
- 意图：`PRODUCT | BILLING | TECH_SUPPORT | POLICY | CHITCHAT | UNKNOWN`。  
- 映射 `csai.router.intent-model-mapping`；失败降级 UNKNOWN + defaultAnswerModelId。  
- 输出 `RoutingDecision`；写入 `cs_route_log`。

### 5.3 `knowledge`

- 上传 PDF/MD/TXT；大小上限可配（默认 10MB）。  
- 切片 + Embedding + `PgVectorStore`。  
- 业务元数据表 `cs_document`；向量表遵循 Spring AI PgVector 约定，metadata 关联 documentId。  
- 检索 topK / threshold 可配；返回 sources。

### 5.4 `chat`

- `POST /api/v1/chat`、`POST /api/v1/chat/stream`。  
- session 最近 N 轮（默认 6）。  
- 编排 Router → RAG → Gateway；持久化消息。  

### 5.5 `admin`

- Thymeleaf：知识库、对话测试台、模型/路由只读。  
- 返回 HTML 视图，**不**走 ApiResponse JSON 包装（视图解析器路径）。

### 5.6 业务 `common`

- 仅放客服域 DTO、`CsaiProperties`、领域异常。  
- **不重复**实现 RequestId/统一响应/日志门面。

---

## 6. 数据模型

### 6.1 表（Flyway）

**cs_document** — id, title, filename, content_type, status, chunk_count, error_message, created_at, updated_at  

**cs_chat_session** — id, title, created_at, updated_at  

**cs_chat_message** — id, session_id, role, content, created_at  

**cs_route_log** — id, session_id, message_id, user_query, intent, confidence, classifier_model_id, answer_model_id, rag_enabled, latency_ms, request_id, created_at  

向量：Spring AI PgVectorStore 表 + metadata(documentId, title, …)。维度与 `csai.embedding.dimensions` 一致（默认 1536）。

主键：实现期统一 **UUID** 或 **Snowflake（common IdGenerator）** 之一并写死；推荐 UUID 与向量 metadata 易读一致。

### 6.2 业务配置（节选）

```yaml
csai:
  models:
    - id: classifier-default
      role: CLASSIFIER
      base-url: ${CS_AI_CLASSIFIER_BASE_URL}
      api-key: ${CS_AI_CLASSIFIER_API_KEY}
      model-name: ${CS_AI_CLASSIFIER_MODEL:gpt-4o-mini}
    - id: answer-strong
      role: ANSWER
      # ...
    - id: answer-fast
      role: ANSWER
      # ...
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

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/csai
    username: csai
    password: ${DB_PASSWORD:csai}
  servlet:
    multipart:
      max-file-size: 10MB

framework:
  common:
    time:
      time-zone: Asia/Shanghai
  web:
    response:
      include-timestamp: true
      include-request-id: true
  database:
    migration:
      enabled: true
      locations: classpath:db/migration
  security:
    path:
      permit-paths:
        - /api/v1/**
        - /admin/**
        - /swagger-ui/**
        - /v3/api-docs/**
        - /actuator/health
```

---

## 7. API 设计

基路径：`/api/v1`  
外层统一 `ApiResponse`；下文描述 **data 载荷**。

| 方法 | 路径 | data 说明 |
|------|------|-----------|
| POST | `/chat` | answer, route, sources, sessionId |
| POST | `/chat/stream` | SSE 增量；结束附 route/sources 元数据 |
| POST | `/knowledge/documents` | 文档元数据 |
| GET | `/knowledge/documents` | 列表（可用框架分页） |
| GET | `/knowledge/documents/{id}` | 详情 |
| DELETE | `/knowledge/documents/{id}` | 删除结果 |
| POST | `/knowledge/search` | 检索调试 |
| GET | `/models` | 模型列表（脱敏） |
| GET | `/router/intents` | 意图与映射 |
| POST | `/router/classify` | 仅分类 |

管理页：`/admin`、`/admin/knowledge`、`/admin/chat`、`/admin/models`。

---

## 8. 错误处理与韧性

| 场景 | 处理 |
|------|------|
| 参数校验失败 | 框架 GlobalExceptionHandler → 标准错误响应 |
| 分类模型失败 | UNKNOWN + default answer 模型；日志含 requestId |
| 回答模型失败 | 可选 fallback 一次；否则业务错误码 |
| 入库失败 | status=FAILED + error_message |
| 检索空 | 仍可调用模型，prompt 约束不编造 |

MVP 不做 Resilience4j 熔断器（可二期接框架/Cloud 组件）。

---

## 9. 安全基线

- apiKey 仅环境变量；日志脱敏（logging-starter masking）。  
- 上传扩展名与大小限制。  
- MVP 路径 permit；生产收紧。  
- 仓库无真实密钥。

---

## 10. 可观测性

- RequestId：`X-Request-ID` / `ApiResponse.requestId`。  
- 日志字段：requestId, sessionId, intent, answerModelId, ragEnabled, sourceCount, latencyMs。  
- Actuator health（含 DB）。  
- route_log 含 request_id 列便于关联。

---

## 11. 测试策略

| 层级 | 内容 |
|------|------|
| 单元 | 意图解析、映射、降级（Mock ChatModel） |
| 集成 | Testcontainers PostgreSQL(pgvector)；上传→检索；编排 |
| 契约 | 响应为 ApiResponse 结构；requestId 存在 |
| 安全 | permit 路径可匿名访问演示接口 |
| mock profile | 无外网 Key 时 CI 可绿 |

---

## 12. 交付物

1. 底座分析 `docs/analysis/microservice-framework-foundation-analysis.md`  
2. PRD / 本设计 / 开发文档 / 验收清单  
3. 源码 + `scripts/install-framework.sh` + `docker-compose.yml` + `.env.example`  
4. 自动化测试  

---

## 13. Key Decisions

| 决策 | 选择 | 理由 |
|------|------|------|
| 技术底座 | microservice-framework 1.0.0-alpha.1 | 用户指定企业组件；版本与 Boot 3.3.13 对齐 |
| 应用形态 | 模块化单体 | 已确认；底座支持后续拆服务 |
| Web 契约 | 框架 ApiResponse | 禁止双轨错误/响应协议 |
| 数据 | PostgreSQL+PgVector + database-starter | 向量与业务同库；Flyway 治理 |
| AI | 应用侧 Spring AI 1.1.8 | 框架未纳管 AI，边界清晰 |
| 安全 | security-starter + MVP permit | 有边界可演进，不阻塞演示 |
| 路由 | LLM 意图分类 | 产品要求 |
| 模型 | OpenAI-compatible only | 覆盖主流端点 |

---

## 14. Open Questions（默认值）

1. 框架制品未发布 Central 时，默认 **源码 install**（脚本化）。  
2. 主键默认 **UUID**。  
3. 知识原文默认 **本地磁盘**；对象存储二期。  
4. Spring Cloud 组件：由 Parent 纳管，MVP **不启用** Nacos/Gateway。

---

## 15. PR Plan

### PR-0: 框架接入与工程骨架

- install-framework 脚本、业务 parent、必选 starters、application.yml（framework.*）、Compose postgres、健康检查、OpenAPI 骨架  
- **验收**：本地 install 框架后业务可启动，`/actuator/health` UP，ApiResponse 示例接口正常  

### PR-1: 模型网关

- ModelRegistry / Gateway、配置绑定、`GET /models`  
- **依赖**：PR-0  

### PR-2: 智能路由

- 意图分类、映射、route_log、classify API  
- **依赖**：PR-1  

### PR-3: 知识库 RAG

- 上传/切片/向量/检索、Flyway 业务表  
- **依赖**：PR-0、PR-1（embedding）  

### PR-4: 对话编排

- chat / stream、session、完整编排  
- **依赖**：PR-1～PR-3  

### PR-5: 管理后台与验收

- Thymeleaf 三页、文档定稿、集成测试、验收清单走查  
- **依赖**：PR-4  

---

## 16. 风险与缓解

| 风险 | 缓解 |
|------|------|
| 框架 alpha 获取方式 | install 脚本 + 文档；锁定版本 |
| Boot 3.3 × Spring AI 1.1 | 集成验证；不升 Boot |
| SSE 被 ResponseBodyAdvice 干扰 | 专项测试与排除策略 |
| Security 误拦 | permitPaths + 集成测试 |
| Embedding 维度不一致 | 配置与迁移说明绑定 |

---

*v1.1 为当前实现真源。实现计划以本 PR Plan 展开。*
