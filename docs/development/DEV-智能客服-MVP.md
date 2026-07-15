# 开发文档 — 智能客服系统 MVP

| 属性 | 值 |
|------|-----|
| 版本 | 1.0 |
| 日期 | 2026-07-15 |
| 关联设计 | `docs/superpowers/specs/2026-07-15-intelligent-customer-service-design.md` |

---

## 1. 工程结构（目标）

```text
customer-service-ai/
├── pom.xml
├── docker-compose.yml
├── .env.example
├── README.md
├── docs/
│   ├── requirements/PRD-智能客服-MVP.md
│   ├── development/DEV-智能客服-MVP.md
│   ├── acceptance/ACCEPTANCE-智能客服-MVP.md
│   └── superpowers/specs/2026-07-15-intelligent-customer-service-design.md
└── src/
    ├── main/java/com/enterprise/csai/
    │   ├── CustomerServiceAiApplication.java
    │   ├── common/          # 异常、配置属性、工具
    │   ├── modelgateway/    # 多模型注册与调用
    │   ├── router/          # 意图分类与路由
    │   ├── knowledge/       # 文档、切片、向量、检索
    │   ├── chat/            # 会话编排、API
    │   └── admin/           # Thymeleaf 控制器
    ├── main/resources/
    │   ├── application.yml
    │   ├── application-mock.yml
    │   ├── db/migration/    # 如使用 Flyway
    │   ├── templates/admin/
    │   └── static/
    └── test/java/...
```

## 2. 技术版本

| 组件 | 版本 |
|------|------|
| OpenJDK | 21 |
| Spring Boot | 3.3.13 |
| Spring Cloud Dependencies | 2023.0.6（或 2023.0 最新补丁） |
| Spring AI BOM | 1.1.8 |
| PostgreSQL / pgvector | 16 + pgvector 镜像 |
| Maven | 3.9+ |
| springdoc-openapi | 与 Boot 3.3 兼容版本 |

### 2.1 核心依赖（示意）

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.3.13</version>
</parent>

<!-- dependencyManagement: spring-ai-bom 1.1.8, spring-cloud-dependencies 2023.0.x -->

<!-- spring-boot-starter-web, validation, thymeleaf, actuator, jdbc/jpa -->
<!-- spring-ai-starter-model-openai -->
<!-- spring-ai-starter-vector-store-pgvector -->
<!-- spring-ai-advisors-vector-store（如使用） -->
<!-- postgresql, flyway, springdoc-openapi-starter-webmvc-ui -->
<!-- test: spring-boot-starter-test, testcontainers -->
```

## 3. 本地运行

### 3.1 前置

- JDK 21、Maven 3.9+、Docker Desktop  
- 可用的 OpenAI-compatible API（或使用 mock profile）

### 3.2 启动基础设施

```bash
cp .env.example .env
# 编辑 .env 填入模型 endpoint 与 key
docker compose up -d postgres
```

### 3.3 启动应用

```bash
# 真实模型
./mvnw spring-boot:run

# 无密钥 CI/本地
./mvnw spring-boot:run -Dspring-boot.run.profiles=mock
```

### 3.4 访问

| 地址 | 说明 |
|------|------|
| http://localhost:8080/admin | 管理后台 |
| http://localhost:8080/swagger-ui.html | OpenAPI |
| http://localhost:8080/actuator/health | 健康检查 |

## 4. 配置说明

见设计文档 §5.2。关键环境变量：

| 变量 | 含义 |
|------|------|
| `CS_AI_CLASSIFIER_*` | 分类模型 |
| `CS_AI_ANSWER_STRONG_*` / `CS_AI_ANSWER_FAST_*` | 回答模型 |
| `CS_AI_EMBEDDING_*` | 向量模型 |
| `SPRING_DATASOURCE_*` | 数据源（Compose 已提供默认） |

**注意：** Embedding `dimensions` 必须与模型输出及 PgVector 列维度一致。

## 5. 模块实现指引

### 5.1 模型网关

1. `@ConfigurationProperties(prefix = "csai")` 绑定模型列表。  
2. 初始化时为每个 enabled 的 chat 模型构建 `OpenAiApi` + `OpenAiChatModel`（或等价 1.1.x API）。  
3. `ModelRegistry` 用 `ConcurrentHashMap<String, ChatModel>` 保存。  
4. Streaming 使用 `ChatModel.stream` / `ChatClient`。

### 5.2 路由

1. 固定 system prompt，要求只输出 JSON。  
2. 使用 `ObjectMapper` 解析；失败 → UNKNOWN。  
3. `RoutingService.route(query) -> RoutingDecision`。

### 5.3 知识库

1. 上传落盘或仅内存字节流解析。  
2. `Tika`/`PdfBox` + 文本切分。  
3. `VectorStore.add(List<Document>)`，metadata 含 documentId、title。  
4. 删除时按 metadata filter 清理（若 store 支持）并删业务表。

### 5.4 对话编排伪代码

```text
function chat(req):
  session = loadOrCreate(req.sessionId)
  history = lastN(session, N)
  decision = router.route(req.message)
  if decision.ragEnabled or forceRag:
    sources = knowledge.search(req.message, topK)
  else:
    sources = []
  prompt = buildPrompt(history, sources, req.message)
  answer = gateway.chat(decision.answerModelId, prompt)
  save messages + route_log
  return { answer, route: decision, sources }
```

## 6. API 示例

### 6.1 同步问答

```bash
curl -s -X POST http://localhost:8080/api/v1/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"如何申请退款？"}' | jq
```

### 6.2 上传知识

```bash
curl -s -X POST http://localhost:8080/api/v1/knowledge/documents \
  -F 'file=@./samples/refund-policy.md' \
  -F 'title=退款政策'
```

### 6.3 检索调试

```bash
curl -s -X POST http://localhost:8080/api/v1/knowledge/search \
  -H 'Content-Type: application/json' \
  -d '{"query":"退款","topK":3}' | jq
```

## 7. 测试指南

```bash
# 单元 + 集成（Testcontainers 需 Docker）
./mvnw test

# 仅单元
./mvnw test -Dgroups=unit
```

**建议用例**

- `IntentParserTest`：合法/非法 JSON  
- `RoutingServiceTest`：映射与降级  
- `KnowledgeServiceIT`：入库与检索  
- `ChatOrchestratorTest`：Mock 网关完整编排  

## 8. 编码规范

- Java 21；构造器注入；避免字段注入。  
- 公共 API DTO 与领域对象分离。  
- 不在日志中输出 apiKey。  
- 新增意图时同步更新：枚举、配置映射、管理台展示、测试。  
- 中文注释仅用于复杂业务意图；代码标识符使用英文。

## 9. 分支与提交建议

按设计文档 PR Plan 分提交：

1. scaffold  
2. model-gateway  
3. router  
4. knowledge-rag  
5. chat-orchestrator  
6. admin-and-docs  

提交信息使用完整句子，说明动机。

## 10. 排障

| 现象 | 排查 |
|------|------|
| 向量维度错误 | 检查 embedding model 与 dimensions |
| 模型 401 | base-url / api-key / 是否需额外 header |
| 分类总是 UNKNOWN | 查看 classifier 原始输出日志；放宽 JSON 解析 |
| PDF 乱码 | 确认文本层 PDF；扫描件需 OCR（MVP 不支持） |
| Compose 连不上 DB | 检查端口 5432、网络、应用 datasource URL |

## 11. 后续演进（非 MVP）

- 拆分 model-gateway / knowledge 为独立服务 + Spring Cloud Gateway/Nacos  
- API Key / OAuth2 鉴权  
- 异步文档索引队列  
- 命中率/满意度运营看板  
- Hybrid 检索（关键词 + 向量）  

---

*实现以设计规格为准；本文侧重工程落地与协作约定。*
