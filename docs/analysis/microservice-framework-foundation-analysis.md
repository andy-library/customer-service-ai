# 技术底座分析：microservice-framework

| 属性 | 值 |
|------|-----|
| 来源 | https://github.com/andy-library/microservice-framework |
| 分析日期 | 2026-07-15 |
| 框架版本 | **1.0.0-alpha.1** |
| 结论 | **作为智能客服项目强制技术基座**；业务只做 AI/客服域能力 |

---

## 1. 框架是什么

`andy-library/microservice-framework` 是一套 **AI 驱动、生产导向** 的企业级 Java 微服务应用基座：

- 统一 **Parent POM / Dependencies BOM / Framework BOM / Starter Parent**
- **19 个** Spring Boot Starter（工程治理、Web、安全、数据、消息、可观测等）
- 独立 **Demo** 做嵌入式 + 真实中间件验收
- 公开 PRD、设计、工程基线与发布规范

业务应用的标准入口：

```xml
<parent>
  <groupId>com.microservice.framework</groupId>
  <artifactId>microservice-framework-starter-parent</artifactId>
  <version>1.0.0-alpha.1</version>
  <relativePath/>
</parent>
```

然后 **按需** 引入 `com.microservice.framework:microservice-framework-*-starter`（版本由 Framework BOM 管理，业务 POM **不写版本号**）。

---

## 2. 技术基线（与客服项目对齐）

| 项 | 框架锁定 | 客服项目 | 结论 |
|----|----------|----------|------|
| Java | 21 | 21 | 完全一致 |
| Spring Boot | **3.3.13** | 3.3.x（勿升 3.4+） | 完全一致 |
| Spring Cloud | **2023.0.6** | 3.3 配套 | 完全一致 |
| Maven | 3.9+ | 3.9+ | 一致 |
| 配置前缀 | `framework.*` | 业务用 `csai.*` | 分层清晰 |
| GroupId | `com.microservice.framework` | 业务 `com.enterprise.csai` | 应用自有 groupId |
| Spring AI | **未纳管** | 1.1.8 | 应用侧 `import` spring-ai-bom |
| 向量库 | 无专用 starter | PgVector | 应用层 + Spring AI PgVector |
| 默认演示库 | MySQL / H2 | PostgreSQL+pgvector | database-starter **支持** `jdbc:postgresql` |

**重要约束：**

- 禁止覆盖受保护属性 `spring-boot.version`（Enforcer 会失败）。
- 第三方版本优先由 Framework Dependencies BOM 管理；**Spring AI 不在 BOM 内**，允许业务在 `dependencyManagement` 中 **额外 import** `spring-ai-bom`，且 **不得** 借机改 Boot/Cloud 版本。
- Nacos 与 Apollo **互斥**，MVP 默认都不用（本地 `application.yml` + env）。
- 外网 API Gateway 属于部署层，不在 Starter 内。

---

## 3. Starter 能力与客服选型

### 3.1 MVP 必选（基础链路）

| Starter | 用途 |
|---------|------|
| `common-starter` | 错误码、分页、`IdGenerator`（Snowflake）、上下文、时间 |
| `json-starter` | 统一 JSON（默认 Jackson） |
| `web-starter` | **ApiResponse** 包装、RequestId、全局异常、校验 |
| `logging-starter` | 结构化日志、脱敏、异步日志 |
| `observability-starter` | Actuator / Metrics / Tracing / Health |

### 3.2 MVP 强烈推荐

| Starter | 用途 |
|---------|------|
| `database-starter` | Hikari 增强、事务门面、**Flyway**（`framework.database.migration`） |
| `async-starter` | 受治理线程池（文档异步索引可后续用） |
| `security-starter` | JWT/路径权限；MVP 用 `permitPaths` 放开演示路径 |

### 3.3 MVP 可选（增强，非阻塞）

| Starter | 用途 | MVP 策略 |
|---------|------|----------|
| `redis-starter` | 限流、缓存会话 | 默认关；Compose 可预留 |
| `object-storage-starter` | 知识原文存 MinIO/S3 | 默认本地落盘，接口预留 |
| `audit-starter` | 管理动作审计 | 二期 |
| `feign-starter` | 拆服务后调用 | 单体阶段不用 |
| `kafka-starter` | 异步索引/事件 | 二期 |
| `elasticsearch-starter` | 混合检索 | 二期备选 |
| `nacos` / `apollo` | 配置中心 | 二期 |
| `xxl-job` / `drools` / `field-encryption` | 任务/规则/字段加密 | 非 MVP |

---

## 4. 对业务设计的关键影响

### 4.1 响应协议必须框架化

`web-starter` 的 `ResponseBodyAdvice` 会把非 `ApiResponse` 返回值包装为：

```json
{
  "code": 0,
  "message": "success",
  "data": { /* 业务负载 */ },
  "timestamp": 1710000000000,
  "requestId": "..."
}
```

因此：

- 业务 Controller **直接返回 DTO** 即可（自动包装），或显式 `ApiResponse.success(dto)`。
- 错误使用 `BusinessException` / 框架异常体系，走 `GlobalExceptionHandler`。
- **SSE 流式**、Thymeleaf 视图、Actuator 需排除自动包装；流式接口返回 `SseEmitter` / `Flux` 时需验证不被包装，必要时路径级关闭或自定义 Advice。

**原设计中的自定义错误体应废弃**，改为框架统一错误响应。

### 4.2 工程入口与构建

```text
1. git clone microservice-framework
2. cd SourceCode/microservice-framework-parent && mvn clean install
3. 客服项目 parent = microservice-framework-starter-parent:1.0.0-alpha.1
4. 引入必选 starters + Spring AI 依赖
```

当前发布为 **pre-release alpha**；若未发布到 Maven Central，开发机/CI **必须先 install 框架**（文档与 Compose 脚本需写明）。

### 4.3 数据层

- 使用 `database-starter` + `spring.datasource` 指向 **PostgreSQL（pgvector 镜像）**。
- 业务表 + Spring AI `PgVectorStore` 同库（或同实例不同 schema，MVP 同库）。
- 表结构用 **Flyway**（`framework.database.migration.enabled=true`），禁止生产 `ddl-auto=update`。
- Demo 用 H2/MySQL 模式仅作参考；客服 MVP **以 PostgreSQL 为准**。

### 4.4 安全

- 引入 `security-starter`。
- MVP：`framework.security.path.permitPaths` 包含  
  `/api/v1/**`、`/admin/**`、`/swagger-ui/**`、`/v3/api-docs/**`、`/actuator/health`  
  （演示友好；验收文档标明生产必须收紧）。
- 生产：JWT Resource Server + 管理端鉴权（非 MVP 实现，但架构预留）。

### 4.5 可观测与日志

- 不再自造日志/RequestId 方案；统一 `X-Request-ID`、`framework.logging`、`framework.observability`。
- 路由/模型调用日志字段：`requestId, sessionId, intent, modelId, latencyMs`（脱敏 apiKey）。

### 4.6 ID 与公共能力

- 主键优先使用 `IdGenerator`（Snowflake）或 UUID；文档层统一约定（实现时二选一并写死）。
- 分页列表使用框架 `PageRequest` / `PageResult` / `PageResponse`。

### 4.7 知识文件存储

- MVP：本地 `storage/knowledge/`。
- 演进：`object-storage-starter`（MinIO）存原文，DB 只存元数据与向量。

### 4.8 Spring AI 边界

框架 **不包含** AI starter。客服项目：

```xml
<dependencyManagement>
  <dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-bom</artifactId>
    <version>1.1.8</version>
    <type>pom</type>
    <scope>import</scope>
  </dependency>
</dependencyManagement>
```

业务模块：`spring-ai-starter-model-openai`、`spring-ai-starter-vector-store-pgvector` 等。

AI 域代码（model-gateway / router / knowledge / chat）全部在业务包 `com.enterprise.csai.*`，**不** 回流改框架除非后续沉淀为 `ai-starter`。

---

## 5. 修订后的目标架构

```
                    ┌─────────────────────────────────────┐
                    │  Admin (Thymeleaf) + REST + OpenAPI  │
                    └──────────────────┬──────────────────┘
                                       │
┌──────────────────────────────────────▼──────────────────────────────────────┐
│              customer-service-ai (业务应用 / 模块化单体)                        │
│  parent: microservice-framework-starter-parent:1.0.0-alpha.1                 │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ Framework Starters: common/json/web/logging/observability/database/    │ │
│  │                     async/security (+ optional redis/object-storage)   │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│  ┌──────────┐ ┌──────────────┐ ┌────────┐ ┌──────────────┐ ┌───────────┐  │
│  │  chat    │ │ model-gateway│ │ router │ │  knowledge   │ │  admin    │  │
│  └──────────┘ └──────────────┘ └────────┘ └──────────────┘ └───────────┘  │
│                         Spring AI 1.1.8 (应用侧)                            │
└───────────────┬─────────────────────────────────────┬─────────────────────┘
                │                                     │
       PostgreSQL + PgVector                 OpenAI-compatible Models
       (Flyway 管理业务表)                    (多端点配置)
```

---

## 6. 与原 MVP 文档的差异清单

| 原设计 | 调整后 |
|--------|--------|
| 直接继承 `spring-boot-starter-parent` | 继承 **framework starter-parent** |
| 自建统一错误体 | 使用 **ApiResponse / GlobalExceptionHandler** |
| 自建 RequestId / 日志规范 | 使用 **web + logging + observability** |
| Spring Cloud 仅 BOM 提及 | 由框架 BOM 纳管；MVP 仍不拆服务 |
| 未定义安全 | **security-starter** + permitPaths 演示策略 |
| 裸 JDBC/自管迁移 | **database-starter + Flyway** |
| 自定义健康检查 | **observability-starter / actuator** |
| 无底座安装步骤 | 文档强制：**先 install 框架再构建业务** |
| PR Plan 从脚手架开始 | 增加 **PR-0：框架接入与基础链路** |

**不变的业务核心：**

- 模块化单体  
- 多模型网关（OpenAI-compatible）  
- LLM 意图路由  
- PgVector RAG + sources  
- 简易管理后台  
- Spring AI 1.1.8  

---

## 7. 风险与缓解

| 风险 | 缓解 |
|------|------|
| 框架 alpha 未上中央仓库 | 仓库 `scripts/install-framework.sh` + CI 步骤 clone/install |
| Spring AI 与 Boot 3.3 官方支持矩阵偏差 | 与原方案相同：应用侧验证；不升 Boot |
| ResponseBodyAdvice 干扰 SSE | 流式接口专项测试；必要时排除路径或返回类型 |
| Security 默认拦截管理台 | 明确 permitPaths；集成测试覆盖匿名访问演示接口 |
| 框架升级破坏兼容 | 锁定 `1.0.0-alpha.1`；升级走变更评估 |

---

## 8. 推荐依赖组合（业务 pom 摘要）

```xml
<!-- parent: microservice-framework-starter-parent:1.0.0-alpha.1 -->

<!-- 基础链路 -->
microservice-framework-common-starter
microservice-framework-json-starter
microservice-framework-web-starter
microservice-framework-logging-starter
microservice-framework-observability-starter
microservice-framework-async-starter
microservice-framework-database-starter
microservice-framework-security-starter

<!-- 业务 Web / 视图 -->
spring-boot-starter-web
spring-boot-starter-thymeleaf
spring-boot-starter-validation
spring-boot-starter-jdbc   <!-- 或 data-jpa，实现时定一种 -->
postgresql
flyway-core
springdoc-openapi-starter-webmvc-ui  <!-- 若 BOM 未管，应用侧指定兼容版本 -->

<!-- Spring AI -->
spring-ai-starter-model-openai
spring-ai-starter-vector-store-pgvector
```

---

## 9. 结论

智能客服 MVP **必须以 microservice-framework 为工程与运行时底座**，在统一 Web/日志/观测/数据库/安全之上实现 Spring AI 业务能力。  
后续文档（PRD / 设计 / 开发 / 验收）均按本分析修订为 **v1.1**。
