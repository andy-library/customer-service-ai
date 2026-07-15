# 开发文档 — 智能客服系统 MVP

| 属性 | 值 |
|------|-----|
| 版本 | **1.1** |
| 日期 | 2026-07-15 |
| 关联设计 | `docs/superpowers/specs/2026-07-15-intelligent-customer-service-design.md` v1.1 |
| 技术底座 | https://github.com/andy-library/microservice-framework `1.0.0-alpha.1` |

### 修订记录

| 版本 | 说明 |
|------|------|
| 1.0 | 初版开发说明 |
| 1.1 | 以 microservice-framework 为强制构建与运行基座 |

---

## 1. 工程结构（目标）

```text
customer-service-ai/   # 本仓库工作区根
├── pom.xml            # parent = microservice-framework-starter-parent
├── docker-compose.yml
├── .env.example
├── scripts/
│   └── install-framework.sh
├── README.md
├── docs/
│   ├── analysis/microservice-framework-foundation-analysis.md
│   ├── requirements/PRD-智能客服-MVP.md
│   ├── development/DEV-智能客服-MVP.md
│   ├── acceptance/ACCEPTANCE-智能客服-MVP.md
│   └── superpowers/specs/2026-07-15-intelligent-customer-service-design.md
└── src/
    ├── main/java/com/enterprise/csai/
    │   ├── CustomerServiceAiApplication.java
    │   ├── common/          # 仅业务属性/DTO/领域异常
    │   ├── modelgateway/
    │   ├── router/
    │   ├── knowledge/
    │   ├── chat/
    │   └── admin/
    ├── main/resources/
    │   ├── application.yml
    │   ├── application-mock.yml
    │   ├── db/migration/    # Flyway
    │   ├── templates/admin/
    │   └── static/
    └── test/java/...
```

---

## 2. 技术版本

| 组件 | 版本 | 来源 |
|------|------|------|
| OpenJDK | 21 | 框架锁定 |
| microservice-framework | **1.0.0-alpha.1** | 企业基座 |
| Spring Boot | 3.3.13 | 框架保护，禁止覆盖 |
| Spring Cloud | 2023.0.6 | 框架 BOM |
| Spring AI BOM | 1.1.8 | **业务** dependencyManagement |
| PostgreSQL / pgvector | 16+ | Compose |
| Maven | 3.9+ | 框架要求 |

### 2.1 业务 pom 要点

```xml
<parent>
  <groupId>com.microservice.framework</groupId>
  <artifactId>microservice-framework-starter-parent</artifactId>
  <version>1.0.0-alpha.1</version>
  <relativePath/>
</parent>

<groupId>com.enterprise.csai</groupId>
<artifactId>customer-service-ai</artifactId>
<version>0.1.0-SNAPSHOT</version>

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.ai</groupId>
      <artifactId>spring-ai-bom</artifactId>
      <version>1.1.8</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<!-- 必选 starters（不要写 version） -->
<!-- common / json / web / logging / observability / async / database / security -->

<!-- Spring AI -->
<!-- spring-ai-starter-model-openai -->
<!-- spring-ai-starter-vector-store-pgvector -->

<!-- web/thymeleaf/jdbc/postgresql/flyway/springdoc -->
```

**禁止：** 覆盖 `spring-boot.version`；给框架 Starter 写死冲突版本。

---

## 3. 环境准备

### 3.1 安装技术底座（必须）

```bash
# 推荐使用仓库脚本（实现阶段添加）
./scripts/install-framework.sh

# 或手动：
git clone https://github.com/andy-library/microservice-framework.git /tmp/microservice-framework
cd /tmp/microservice-framework/SourceCode/microservice-framework-parent
mvn clean install -DskipTests
```

验证本地仓库存在：

```bash
ls ~/.m2/repository/com/microservice/framework/microservice-framework-starter-parent/1.0.0-alpha.1/
```

### 3.2 启动中间件与应用

```bash
cp .env.example .env
# 填入模型 endpoint / key
docker compose up -d postgres
./mvnw spring-boot:run
# 无密钥：
./mvnw spring-boot:run -Dspring-boot.run.profiles=mock
```

### 3.3 访问

| 地址 | 说明 |
|------|------|
| http://localhost:8080/admin | 管理后台 |
| http://localhost:8080/swagger-ui.html | OpenAPI（路径以实现为准） |
| http://localhost:8080/actuator/health | 健康检查 |

---

## 4. 配置约定

### 4.1 framework.*（基座）

参照 Demo `application.yml` 与设计规格 §6.2：时区、Web 响应、DB migration、security permitPaths、logging masking 等。

### 4.2 csai.*（业务）

模型列表、路由映射、RAG、embedding、history 窗口等。**密钥仅 env。**

### 4.3 数据源

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/csai
    username: csai
    password: ${DB_PASSWORD}
```

database-starter 增强 Hikari 默认值；迁移：

```yaml
framework:
  database:
    migration:
      enabled: true
      locations: classpath:db/migration
```

---

## 5. 模块实现指引

### 5.1 框架能力复用（不要重造）

| 需求 | 使用 |
|------|------|
| 统一响应 | 返回 DTO，由 web-starter 包装为 ApiResponse |
| 业务错误 | `BusinessException` 等 |
| RequestId | RequestIdFilter / 上下文自动处理 |
| 日志脱敏 | logging-starter |
| 分布式 ID | common `IdGenerator`（若采用） |
| 事务 | `TransactionTemplateFacade` 或 `@Transactional` |
| 健康/指标 | observability-starter |

### 5.2 模型网关 / 路由 / RAG / 对话

业务逻辑同设计规格 §5；调用链日志必须带 `requestId`。

### 5.3 SSE 注意

验证流式接口不被 `ResponseBodyAdvice` 错误包装。若出现问题：

- 确认返回类型为 `SseEmitter` / 流式类型；  
- 或对该路径关闭包装 / 自定义 Advice 排除。

### 5.4 安全

MVP 使用 permitPaths 放开演示接口。引入 security-starter 后务必配置，否则可能 401 全站。

---

## 6. API 示例

```bash
# 同步问答 —— 注意外层 ApiResponse
curl -s -X POST http://localhost:8080/api/v1/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"如何申请退款？"}' | jq

# 期望：.code == 0 且 .data.answer 存在，.requestId 非空
```

```bash
curl -s -X POST http://localhost:8080/api/v1/knowledge/documents \
  -F 'file=@./samples/refund-policy.md' \
  -F 'title=退款政策'
```

---

## 7. 测试指南

```bash
./mvnw test
./mvnw verify   # 含 IT（若配置 failsafe）
```

建议用例：

- `IntentParserTest`、`RoutingServiceTest`  
- `KnowledgeServiceIT`（Testcontainers pgvector）  
- `ChatOrchestratorTest`  
- `ApiResponseContractTest`（MockMvc 断言 code/data/requestId）  
- `SecurityPermitPathIT`  

---

## 8. 编码规范

- 业务包：`com.enterprise.csai.**`  
- 不复制框架源码进业务仓库；只依赖 Starter 坐标。  
- 不覆盖框架保护版本属性。  
- 新增意图时同步：枚举、配置、管理台、测试。  
- 中文注释仅用于复杂业务语义；标识符英文。

---

## 9. 分支与提交建议

1. PR-0 框架接入与骨架  
2. 模型网关  
3. 路由  
4. RAG  
5. 对话编排  
6. 管理台与验收  

---

## 10. 排障

| 现象 | 排查 |
|------|------|
| 找不到 `com.microservice.framework:*` | 未 install 框架；检查 `~/.m2` |
| Enforcer 失败 / Boot 版本被改 | 删除业务对 `spring-boot.version` 的覆盖 |
| 全站 401 | security permitPaths |
| 响应不是 ApiResponse | 是否排除了 web-starter 或返回了 String 原始体 |
| 向量维度错误 | embedding dimensions 与表定义 |
| 模型 401 | base-url / api-key |

---

## 11. 后续演进

- 配置中心：Nacos **或** Apollo（互斥）  
- 拆分 knowledge / model-gateway 服务 + Feign  
- object-storage 存原文；audit 管理操作  
- 回馈框架：评估 `ai-starter`  

---

*实现以设计规格 v1.1 为准；本文侧重框架接入与工程约定。*
