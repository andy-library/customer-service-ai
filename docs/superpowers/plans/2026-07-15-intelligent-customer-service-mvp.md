# Intelligent Customer Service MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a modular-monolith intelligent customer service app on `microservice-framework` 1.0.0-alpha.1 with multi-model OpenAI-compatible gateway, LLM intent routing, PgVector RAG, REST+SSE chat, and a simple Thymeleaf admin UI.

**Architecture:** Single Spring Boot app (`customer-service-ai`) inherits `microservice-framework-starter-parent`. Framework starters supply ApiResponse, RequestId, logging, observability, database/Flyway, and security permit paths. Business packages under `com.enterprise.csai` implement model-gateway, router, knowledge, chat, and admin. Spring AI 1.1.8 is imported only at the application layer.

**Tech Stack:** OpenJDK 21, Spring Boot 3.3.13, Spring Cloud 2023.0.6 (via framework), Spring AI 1.1.8, PostgreSQL+PgVector, Maven 3.9+, Testcontainers, Thymeleaf, springdoc-openapi.

**Spec sources:**
- `docs/superpowers/specs/2026-07-15-intelligent-customer-service-design.md` (v1.1)
- `docs/analysis/microservice-framework-foundation-analysis.md`
- `docs/requirements/PRD-智能客服-MVP.md` (v1.1)
- `docs/development/DEV-智能客服-MVP.md` (v1.1)
- `docs/acceptance/ACCEPTANCE-智能客服-MVP.md` (v1.1)

## Global Constraints

- Parent MUST be `com.microservice.framework:microservice-framework-starter-parent:1.0.0-alpha.1`.
- MUST NOT override protected property `spring-boot.version` (must remain 3.3.13).
- Framework starter dependencies MUST omit `<version>` (BOM-managed).
- Spring AI BOM `1.1.8` is imported only in the app `dependencyManagement`; do not use Spring AI 2.x.
- Business package root: `com.enterprise.csai.**` only.
- JSON REST responses use framework `ApiResponse` (success code `0`); do not invent a second envelope.
- Primary DB: PostgreSQL with pgvector; schema via Flyway (`framework.database.migration.enabled=true`).
- Model protocol: OpenAI-compatible only; secrets only via env vars / `.env` (never commit real keys).
- MVP security: `framework.security.path.permit-paths` includes `/api/v1/**`, `/admin/**`, swagger, and health.
- Tests: JUnit 5 + AssertJ + Mockito; integration tests use Testcontainers when Docker is available; provide `mock` profile for CI without external LLM keys.
- Commits: small, frequent, conventional messages (`feat:`, `test:`, `docs:`, `chore:`).

---

## File Map (create unless noted)

```text
customer-service-ai/   # workspace root = this repo
├── pom.xml
├── .gitignore
├── .env.example
├── README.md
├── docker-compose.yml
├── scripts/install-framework.sh
├── samples/refund-policy.md
├── src/main/java/com/enterprise/csai/
│   ├── CustomerServiceAiApplication.java
│   ├── common/
│   │   ├── config/CsaiProperties.java
│   │   ├── config/CsaiConfiguration.java
│   │   ├── error/CsaiErrorCodes.java
│   │   └── dto/ (shared small DTOs if needed)
│   ├── modelgateway/
│   │   ├── ModelRole.java
│   │   ├── ModelDefinition.java
│   │   ├── ModelRegistry.java
│   │   ├── ModelGateway.java
│   │   ├── ModelGatewayService.java
│   │   ├── ModelGatewayConfiguration.java
│   │   └── api/ModelController.java
│   ├── router/
│   │   ├── IntentType.java
│   │   ├── RoutingDecision.java
│   │   ├── IntentClassifier.java
│   │   ├── IntentClassifierImpl.java
│   │   ├── RoutingService.java
│   │   ├── RouteLogEntity.java
│   │   ├── RouteLogRepository.java
│   │   └── api/RouterController.java
│   ├── knowledge/
│   │   ├── DocumentStatus.java
│   │   ├── DocumentEntity.java
│   │   ├── DocumentRepository.java
│   │   ├── KnowledgeChunk.java
│   │   ├── DocumentIngestService.java
│   │   ├── KnowledgeSearchService.java
│   │   ├── TextExtractionService.java
│   │   └── api/KnowledgeController.java
│   ├── chat/
│   │   ├── ChatSessionEntity.java
│   │   ├── ChatMessageEntity.java
│   │   ├── ChatSessionRepository.java
│   │   ├── ChatMessageRepository.java
│   │   ├── ChatRequest.java
│   │   ├── ChatResponse.java
│   │   ├── SourceDto.java
│   │   ├── ChatOrchestrator.java
│   │   └── api/ChatController.java
│   └── admin/
│       └── AdminController.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-mock.yml
│   ├── db/migration/V1__init_csai.sql
│   ├── templates/admin/{index,knowledge,chat,models}.html
│   └── static/admin/app.css
└── src/test/java/com/enterprise/csai/
    ├── modelgateway/ModelRegistryTest.java
    ├── router/IntentClassifierParserTest.java
    ├── router/RoutingServiceTest.java
    ├── knowledge/ (unit as needed)
    ├── chat/ChatOrchestratorTest.java
    ├── web/ApiResponseContractTest.java
    └── support/MockChatModelConfig.java
```

---

### Task 1: Framework install script + Maven skeleton + Compose

**Files:**
- Create: `scripts/install-framework.sh`
- Create: `pom.xml`
- Create: `.gitignore`
- Create: `.env.example`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/enterprise/csai/CustomerServiceAiApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-mock.yml`
- Create: `src/test/java/com/enterprise/csai/ContextLoadsTest.java`
- Create: `README.md` (minimal run steps)

**Interfaces:**
- Consumes: local Maven install of `microservice-framework-starter-parent:1.0.0-alpha.1`
- Produces: bootable empty app with framework starters on classpath

- [ ] **Step 1: Create framework install script**

```bash
#!/usr/bin/env bash
# scripts/install-framework.sh
set -euo pipefail
FRAMEWORK_VERSION="${FRAMEWORK_VERSION:-1.0.0-alpha.1}"
CLONE_DIR="${FRAMEWORK_CLONE_DIR:-/tmp/microservice-framework}"
REPO_URL="${FRAMEWORK_REPO_URL:-https://github.com/andy-library/microservice-framework.git}"

if [[ -d "${HOME}/.m2/repository/com/microservice/framework/microservice-framework-starter-parent/${FRAMEWORK_VERSION}" ]]; then
  echo "Framework ${FRAMEWORK_VERSION} already installed in local m2."
  exit 0
fi

if [[ ! -d "${CLONE_DIR}/.git" ]]; then
  git clone --depth 1 --branch "v${FRAMEWORK_VERSION}" "${REPO_URL}" "${CLONE_DIR}" \
    || git clone --depth 1 "${REPO_URL}" "${CLONE_DIR}"
fi

cd "${CLONE_DIR}/SourceCode/microservice-framework-parent"
mvn clean install -DskipTests -B
echo "Installed microservice-framework ${FRAMEWORK_VERSION}"
```

Make executable: `chmod +x scripts/install-framework.sh`

- [ ] **Step 2: Run install script**

Run: `./scripts/install-framework.sh`  
Expected: ends with install success; directory exists under `~/.m2/repository/com/microservice/framework/...`

- [ ] **Step 3: Write `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.microservice.framework</groupId>
    <artifactId>microservice-framework-starter-parent</artifactId>
    <version>1.0.0-alpha.1</version>
    <relativePath/>
  </parent>

  <groupId>com.enterprise.csai</groupId>
  <artifactId>customer-service-ai</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <name>customer-service-ai</name>
  <description>Intelligent customer service MVP on microservice-framework + Spring AI</description>

  <properties>
    <spring-ai.version>1.1.8</spring-ai.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-bom</artifactId>
        <version>${spring-ai.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>

    <dependency>
      <groupId>com.microservice.framework</groupId>
      <artifactId>microservice-framework-common-starter</artifactId>
    </dependency>
    <dependency>
      <groupId>com.microservice.framework</groupId>
      <artifactId>microservice-framework-json-starter</artifactId>
    </dependency>
    <dependency>
      <groupId>com.microservice.framework</groupId>
      <artifactId>microservice-framework-web-starter</artifactId>
    </dependency>
    <dependency>
      <groupId>com.microservice.framework</groupId>
      <artifactId>microservice-framework-logging-starter</artifactId>
    </dependency>
    <dependency>
      <groupId>com.microservice.framework</groupId>
      <artifactId>microservice-framework-observability-starter</artifactId>
    </dependency>
    <dependency>
      <groupId>com.microservice.framework</groupId>
      <artifactId>microservice-framework-async-starter</artifactId>
    </dependency>
    <dependency>
      <groupId>com.microservice.framework</groupId>
      <artifactId>microservice-framework-database-starter</artifactId>
    </dependency>
    <dependency>
      <groupId>com.microservice.framework</groupId>
      <artifactId>microservice-framework-security-starter</artifactId>
    </dependency>

    <dependency>
      <groupId>org.springframework.ai</groupId>
      <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.ai</groupId>
      <artifactId>spring-ai-starter-vector-store-pgvector</artifactId>
    </dependency>

    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    <dependency>
      <groupId>org.apache.pdfbox</groupId>
      <artifactId>pdfbox</artifactId>
      <version>3.0.3</version>
    </dependency>
    <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
      <version>2.6.0</version>
    </dependency>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>postgresql</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

Notes for implementer:
- If enforcer complains about `pdfbox` / `springdoc` versions, move versions into a comment and pick coordinates already managed by framework BOM, or add only if allowed.
- If `flyway-database-postgresql` is not resolved, keep `flyway-core` only and re-test.
- Do **not** set `<spring-boot.version>` in properties.

- [ ] **Step 4: Write application entry + config + compose**

`CustomerServiceAiApplication.java`:

```java
package com.enterprise.csai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CustomerServiceAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceAiApplication.class, args);
    }
}
```

`application.yml` (core skeleton — expand in later tasks):

```yaml
server:
  port: 8080

spring:
  application:
    name: customer-service-ai
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:csai}
    username: ${DB_USER:csai}
    password: ${DB_PASSWORD:csai}
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 12MB
  ai:
    openai:
      api-key: ${CS_AI_DEFAULT_API_KEY:sk-placeholder}
      base-url: ${CS_AI_DEFAULT_BASE_URL:https://api.openai.com}
    vectorstore:
      pgvector:
        initialize-schema: true
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: ${CS_AI_EMBEDDING_DIMENSIONS:1536}

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
        - /swagger-ui.html
        - /v3/api-docs/**
        - /actuator/health
        - /actuator/info

csai:
  models: []
  embedding:
    base-url: ${CS_AI_EMBEDDING_BASE_URL:${CS_AI_DEFAULT_BASE_URL:https://api.openai.com}}
    api-key: ${CS_AI_EMBEDDING_API_KEY:${CS_AI_DEFAULT_API_KEY:sk-placeholder}}
    model-name: ${CS_AI_EMBEDDING_MODEL:text-embedding-3-small}
    dimensions: 1536
  router:
    classifier-model-id: classifier-default
    default-answer-model-id: answer-fast
    force-rag: false
    intent-model-mapping: {}
  rag:
    top-k: 5
    similarity-threshold: 0.50
    chunk-size: 800
    chunk-overlap: 100
  chat:
    history-max-messages: 6
  upload:
    max-bytes: 10485760
    allowed-extensions:
      - pdf
      - md
      - txt

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

`docker-compose.yml`:

```yaml
services:
  postgres:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: csai
      POSTGRES_USER: csai
      POSTGRES_PASSWORD: csai
    ports:
      - "5432:5432"
    volumes:
      - csai_pg:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U csai -d csai"]
      interval: 5s
      timeout: 5s
      retries: 10

volumes:
  csai_pg:
```

`.env.example`:

```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=csai
DB_USER=csai
DB_PASSWORD=csai

CS_AI_DEFAULT_BASE_URL=https://api.openai.com
CS_AI_DEFAULT_API_KEY=replace-me

CS_AI_CLASSIFIER_BASE_URL=
CS_AI_CLASSIFIER_API_KEY=
CS_AI_CLASSIFIER_MODEL=gpt-4o-mini

CS_AI_ANSWER_STRONG_BASE_URL=
CS_AI_ANSWER_STRONG_API_KEY=
CS_AI_ANSWER_STRONG_MODEL=

CS_AI_ANSWER_FAST_BASE_URL=
CS_AI_ANSWER_FAST_API_KEY=
CS_AI_ANSWER_FAST_MODEL=

CS_AI_EMBEDDING_BASE_URL=
CS_AI_EMBEDDING_API_KEY=
CS_AI_EMBEDDING_MODEL=text-embedding-3-small
CS_AI_EMBEDDING_DIMENSIONS=1536
```

`.gitignore`: include `target/`, `.env`, `.idea/`, `*.iml`, `.DS_Store`, `storage/`.

- [ ] **Step 5: Write smoke test (context may need DB — use mock profile or Testcontainers later)**

For Task 1, prefer a pure unit that validates parent resolution is not required at runtime. Create:

```java
package com.enterprise.csai;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PomCoordinatesSanityTest {
    @Test
    void springAiVersionPropertyIsPinned() {
        // Documentation guard: Spring AI major must stay on 1.x for Boot 3.3
        assertThat("1.1.8").startsWith("1.");
    }
}
```

Full `@SpringBootTest` is introduced in Task 2 after Flyway migration exists (or with Testcontainers).

- [ ] **Step 6: Verify Maven resolves**

Run:

```bash
./scripts/install-framework.sh
mvn -q -DskipTests dependency:resolve
mvn -q -Dtest=PomCoordinatesSanityTest test
```

Expected: BUILD SUCCESS.

If framework artifacts missing: fix install script tag/branch and re-run.

- [ ] **Step 7: Commit**

```bash
git add scripts/install-framework.sh pom.xml .gitignore .env.example docker-compose.yml \
  src/main/java/com/enterprise/csai/CustomerServiceAiApplication.java \
  src/main/resources/application.yml src/main/resources/application-mock.yml \
  src/test/java/com/enterprise/csai/PomCoordinatesSanityTest.java README.md
git commit -m "chore: scaffold customer-service-ai on microservice-framework parent"
```

---

### Task 2: CsaiProperties + Flyway schema + ApiResponse contract endpoint

**Files:**
- Create: `src/main/java/com/enterprise/csai/common/config/CsaiProperties.java`
- Create: `src/main/java/com/enterprise/csai/common/config/CsaiConfiguration.java`
- Create: `src/main/java/com/enterprise/csai/common/error/CsaiErrorCodes.java`
- Create: `src/main/resources/db/migration/V1__init_csai.sql`
- Create: `src/main/java/com/enterprise/csai/common/api/PingController.java`
- Create: `src/test/java/com/enterprise/csai/web/ApiResponseContractTest.java`

**Interfaces:**
- Produces: `CsaiProperties` bound from `csai.*`
- Produces: tables `cs_document`, `cs_chat_session`, `cs_chat_message`, `cs_route_log`
- Produces: `GET /api/v1/ping` → body wrapped as ApiResponse

- [ ] **Step 1: Write failing contract test**

```java
package com.enterprise.csai.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ApiResponseContractTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("csai")
            .withUsername("csai")
            .withPassword("csai");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.ai.openai.api-key", () -> "test-key");
        registry.add("spring.ai.openai.base-url", () -> "http://localhost:9");
    }

    @Autowired MockMvc mockMvc;

    @Test
    void pingIsWrappedAsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("pong"))
                .andExpect(jsonPath("$.requestId").exists());
    }
}
```

- [ ] **Step 2: Run test — expect fail** (missing mapping or context fail)

Run: `mvn -Dtest=ApiResponseContractTest test`  
Expected: FAIL (404 or context error).

- [ ] **Step 3: Implement properties, migration, ping**

`CsaiProperties.java` — nested records/classes for `models`, `embedding`, `router`, `rag`, `chat`, `upload` matching design YAML keys (`base-url` → `baseUrl`, etc.). Use `@ConfigurationProperties(prefix = "csai")` + `@Validated`.

`ModelRole` enum values: `CLASSIFIER`, `ANSWER`, `EMBEDDING`.

`V1__init_csai.sql`:

```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE cs_document (
    id              UUID PRIMARY KEY,
    title           VARCHAR(512) NOT NULL,
    filename        VARCHAR(512) NOT NULL,
    content_type    VARCHAR(128),
    status          VARCHAR(32)  NOT NULL,
    chunk_count     INT          NOT NULL DEFAULT 0,
    error_message   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE cs_chat_session (
    id          UUID PRIMARY KEY,
    title       VARCHAR(512),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE cs_chat_message (
    id          UUID PRIMARY KEY,
    session_id  UUID NOT NULL REFERENCES cs_chat_session(id) ON DELETE CASCADE,
    role        VARCHAR(32) NOT NULL,
    content     TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_cs_chat_message_session ON cs_chat_message(session_id, created_at);

CREATE TABLE cs_route_log (
    id                    UUID PRIMARY KEY,
    session_id            UUID,
    message_id            UUID,
    user_query            TEXT NOT NULL,
    intent                VARCHAR(64) NOT NULL,
    confidence            DOUBLE PRECISION,
    classifier_model_id   VARCHAR(128),
    answer_model_id       VARCHAR(128),
    rag_enabled           BOOLEAN NOT NULL DEFAULT FALSE,
    latency_ms            BIGINT,
    request_id            VARCHAR(128),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

`PingController`:

```java
@RestController
@RequestMapping("/api/v1")
public class PingController {
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
```

- [ ] **Step 4: Re-run contract test**

Run: `mvn -Dtest=ApiResponseContractTest test`  
Expected: PASS (Docker required). If security blocks: fix `permit-paths`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/enterprise/csai/common src/main/resources/db \
  src/test/java/com/enterprise/csai/web
git commit -m "feat: add csai properties, flyway schema, and ApiResponse ping contract"
```

---

### Task 3: Model gateway (registry + invocation)

**Files:**
- Create: `modelgateway/*` as in File Map
- Create: `src/test/java/com/enterprise/csai/modelgateway/ModelRegistryTest.java`
- Modify: `application.yml` sample `csai.models` entries (using env placeholders)

**Interfaces:**
- Produces:
  - `enum ModelRole { CLASSIFIER, ANSWER, EMBEDDING }`
  - `record ModelDefinition(String id, String displayName, String baseUrl, String apiKey, String modelName, ModelRole role, boolean enabled, long timeoutMs)`
  - `interface ModelRegistry { Optional<ChatModel> getChatModel(String id); List<ModelView> listModels(); String require(String id); }`
  - `interface ModelGateway { String chat(String modelId, List<Message> messages); Flux<String> stream(String modelId, List<Message> messages); }`
  - `GET /api/v1/models` → list without apiKey

- [ ] **Step 1: Write failing unit test for registry**

```java
@Test
void listsEnabledModelsWithoutExposingApiKey() {
    CsaiProperties props = new CsaiProperties();
    // set two models with api keys
    ModelRegistry registry = new ModelRegistry(props, /* factory */ mockFactory);
    List<ModelView> views = registry.listModels();
    assertThat(views).hasSize(2);
    assertThat(views).allSatisfy(v -> assertThat(v.apiKey()).isNull());
}
```

Implement `ModelView` as a public record: `id, displayName, modelName, role, enabled` (no apiKey field at all is better than null).

- [ ] **Step 2: Run test — FAIL**

- [ ] **Step 3: Implement ModelGatewayConfiguration**

Build `ChatModel` per enabled definition using Spring AI OpenAI client APIs for 1.1.x:

```java
// Pseudocode — use actual 1.1.8 types after IDE resolve:
// OpenAiApi.builder().baseUrl(...).apiKey(...).build();
// OpenAiChatModel.builder().openAiApi(api).defaultOptions(
//   OpenAiChatOptions.builder().model(modelName).build()).build();
```

If multi-bean creation is awkward with auto-config, **disable conflicting single auto ChatModel** only if needed via properties; prefer manual beans in `ModelGatewayConfiguration` keyed by id.

`ModelGatewayService.chat`:

```java
public String chat(String modelId, List<org.springframework.ai.chat.messages.Message> messages) {
    ChatModel model = registry.getChatModel(modelId)
        .orElseThrow(() -> new BusinessException(CsaiErrorCodes.MODEL_NOT_FOUND, "model not found: " + modelId));
    Prompt prompt = new Prompt(messages);
    return model.call(prompt).getResult().getOutput().getText();
}
```

Use exact Spring AI 1.1.8 message/output getters available in the dependency (adjust `getText()` vs `getContent()` after compile).

- [ ] **Step 4: Controller**

```java
@RestController
@RequestMapping("/api/v1/models")
public class ModelController {
    private final ModelRegistry registry;
    @GetMapping
    public List<ModelView> list() { return registry.listModels(); }
}
```

- [ ] **Step 5: Unit tests pass**

Run: `mvn -Dtest=ModelRegistryTest test`  
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git commit -am "feat: add multi-model gateway registry and models API"
```

---

### Task 4: LLM intent router

**Files:**
- Create: `router/*` as in File Map
- Create: `src/test/java/com/enterprise/csai/router/IntentClassifierParserTest.java`
- Create: `src/test/java/com/enterprise/csai/router/RoutingServiceTest.java`

**Interfaces:**
- Produces:
  - `enum IntentType { PRODUCT, BILLING, TECH_SUPPORT, POLICY, CHITCHAT, UNKNOWN; boolean requiresKnowledge(); }`
  - `record RoutingDecision(IntentType intent, double confidence, String reason, String classifierModelId, String answerModelId, boolean ragEnabled)`
  - `RoutingService.route(String userQuery): RoutingDecision`
  - `POST /api/v1/router/classify` body `{"message":"..."}` → decision
  - `GET /api/v1/router/intents` → mapping view

- [ ] **Step 1: Failing parser tests**

```java
@Test
void parsesValidJson() {
    String raw = "{\"intent\":\"PRODUCT\",\"confidence\":0.91,\"reason\":\"product feature\"}";
    ClassificationResult r = IntentJsonParser.parse(raw);
    assertThat(r.intent()).isEqualTo(IntentType.PRODUCT);
    assertThat(r.confidence()).isEqualTo(0.91);
}

@Test
void invalidJsonBecomesUnknown() {
    assertThat(IntentJsonParser.parse("not-json").intent()).isEqualTo(IntentType.UNKNOWN);
}
```

- [ ] **Step 2: Implement parser + RoutingService**

Classifier system prompt (fixed):

```text
You are an intent classifier for enterprise customer support.
Classify the user message into exactly one intent:
PRODUCT, BILLING, TECH_SUPPORT, POLICY, CHITCHAT, UNKNOWN.
Reply with ONLY compact JSON:
{"intent":"...", "confidence":0.0, "reason":"..."}
```

`RoutingService`:

```java
public RoutingDecision route(String userQuery) {
    String classifierId = props.getRouter().getClassifierModelId();
    try {
        String raw = modelGateway.chat(classifierId, List.of(
            new SystemMessage(SYSTEM),
            new UserMessage(userQuery)
        ));
        ClassificationResult cr = IntentJsonParser.parse(raw);
        String answerId = props.getRouter().getIntentModelMapping()
            .getOrDefault(cr.intent().name(), props.getRouter().getDefaultAnswerModelId());
        boolean rag = props.getRouter().isForceRag() || cr.intent().requiresKnowledge();
        return new RoutingDecision(cr.intent(), cr.confidence(), cr.reason(), classifierId, answerId, rag);
    } catch (Exception ex) {
        log.warn("classifier failed, degrading to UNKNOWN", ex);
        return new RoutingDecision(IntentType.UNKNOWN, 0.0, "classifier_failed",
            classifierId, props.getRouter().getDefaultAnswerModelId(), true);
    }
}
```

`IntentType.requiresKnowledge()`: false only for `CHITCHAT`; true otherwise.

- [ ] **Step 3: RoutingServiceTest with mocked ModelGateway**

```java
when(gateway.chat(eq("classifier-default"), any()))
  .thenReturn("{\"intent\":\"CHITCHAT\",\"confidence\":0.8,\"reason\":\"hi\"}");
RoutingDecision d = routingService.route("你好");
assertThat(d.intent()).isEqualTo(IntentType.CHITCHAT);
assertThat(d.answerModelId()).isEqualTo("answer-fast");
assertThat(d.ragEnabled()).isFalse();
```

- [ ] **Step 4: Controllers + RouteLogRepository (JDBC)**

Use `JdbcClient` / `NamedParameterJdbcTemplate` to insert `cs_route_log` (persist from chat task; classify endpoint may also write).

- [ ] **Step 5: Run tests**

`mvn -Dtest=IntentClassifierParserTest,RoutingServiceTest test` → PASS

- [ ] **Step 6: Commit**

```bash
git commit -am "feat: add LLM intent router with mapping and degrade path"
```

---

### Task 5: Knowledge ingest + PgVector search

**Files:**
- Create: `knowledge/*` as in File Map
- Create: `samples/refund-policy.md`
- Create: `src/test/java/com/enterprise/csai/knowledge/TextExtractionServiceTest.java`
- Optional IT: `KnowledgeSearchServiceIT` with Testcontainers + mock embedding if possible

**Interfaces:**
- Produces:
  - `DocumentIngestService.ingest(MultipartFile file, String title): DocumentEntity`
  - `KnowledgeSearchService.search(String query, int topK): List<KnowledgeChunk>`
  - `record KnowledgeChunk(String documentId, String title, String content, double score)`
  - REST under `/api/v1/knowledge/**`

- [ ] **Step 1: Sample knowledge file**

`samples/refund-policy.md`:

```markdown
# 退款政策
1. 购买后 7 日内未激活可全额退款。
2. 已激活服务按剩余天数折算，手续费 5%。
3. 提交工单选择「账单/退款」类别。
```

- [ ] **Step 2: Text extraction unit test**

```java
@Test
void extractsMarkdownBytes() {
    byte[] bytes = "# Hello\nWorld".getBytes(StandardCharsets.UTF_8);
    String text = extraction.extract("x.md", "text/markdown", bytes);
    assertThat(text).contains("Hello");
}
```

- [ ] **Step 3: Implement extraction**

- `.txt` / `.md`: UTF-8 string  
- `.pdf`: PDFBox `Loader.loadPDF` → strip text  
- else: throw `BusinessException` unsupported type

- [ ] **Step 4: Ingest service**

Flow:
1. Validate extension + size against `csai.upload`
2. Insert `cs_document` status `PENDING`
3. Extract → split with `TokenTextSplitter` (or `TextSplitter`) using `chunk-size` / `chunk-overlap`
4. Build `List<Document>` with metadata `documentId`, `title`, `filename`
5. `vectorStore.add(docs)`
6. Update status `INDEXED` + `chunk_count` OR `FAILED` + `error_message`

Delete: remove DB row; delete vectors by metadata filter if API supports, else document limitation in README.

- [ ] **Step 5: Search service**

```java
List<Document> docs = vectorStore.similaritySearch(
    SearchRequest.builder().query(query).topK(topK).similarityThreshold(threshold).build());
// map to KnowledgeChunk
```

- [ ] **Step 6: KnowledgeController**

| Method | Path |
|--------|------|
| POST | `/api/v1/knowledge/documents` multipart |
| GET | `/api/v1/knowledge/documents` |
| GET | `/api/v1/knowledge/documents/{id}` |
| DELETE | `/api/v1/knowledge/documents/{id}` |
| POST | `/api/v1/knowledge/search` |

- [ ] **Step 7: Unit tests pass; manual optional with real embedding**

`mvn -Dtest=TextExtractionServiceTest test` → PASS

- [ ] **Step 8: Commit**

```bash
git commit -am "feat: add knowledge ingest and pgvector search APIs"
```

---

### Task 6: Chat orchestrator (sync + session)

**Files:**
- Create: `chat/*` as in File Map
- Create: `src/test/java/com/enterprise/csai/chat/ChatOrchestratorTest.java`

**Interfaces:**
- Produces:
  - `record ChatRequest(UUID sessionId, @NotBlank String message, ChatOptions options)`
  - `record ChatOptions(Boolean forceRag, String overrideAnswerModelId)`
  - `record ChatResponse(UUID sessionId, String answer, RoutingDecision route, List<SourceDto> sources)`
  - `record SourceDto(String documentId, String title, String snippet, double score)`
  - `ChatOrchestrator.chat(ChatRequest): ChatResponse`
  - `POST /api/v1/chat`

- [ ] **Step 1: Orchestrator unit test with mocks**

```java
when(routingService.route("如何退款")).thenReturn(new RoutingDecision(
    IntentType.BILLING, 0.9, "billing", "classifier-default", "answer-strong", true));
when(knowledgeSearchService.search(eq("如何退款"), anyInt()))
    .thenReturn(List.of(new KnowledgeChunk(docId, "退款政策", "7 日内未激活可全额退款", 0.88)));
when(modelGateway.chat(eq("answer-strong"), any())).thenReturn("根据知识库，7 日内未激活可全额退款。");

ChatResponse resp = orchestrator.chat(new ChatRequest(null, "如何退款", null));
assertThat(resp.answer()).contains("退款");
assertThat(resp.sources()).isNotEmpty();
assertThat(resp.route().answerModelId()).isEqualTo("answer-strong");
```

- [ ] **Step 2: Implement ChatOrchestrator**

```text
session = loadOrCreate(sessionId)
history = lastN(session, historyMaxMessages)
decision = routingService.route(message)
if options.overrideAnswerModelId != null -> use override
rag = decision.ragEnabled || Boolean.TRUE.equals(options.forceRag)
sources = rag ? knowledge.search(message, topK) : empty
messages = [system(with RAG rules + sources), ...history, user(message)]
answer = gateway.chat(answerModelId, messages)
save user+assistant messages
save route_log with requestId from RequestIdContext
return ChatResponse
```

System prompt rules:
- Prefer sources; if empty sources and rag, say knowledge insufficient.
- Do not invent internal policy numbers not present in sources.

- [ ] **Step 3: ChatController**

```java
@PostMapping("/api/v1/chat")
public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
    return orchestrator.chat(request);
}
```

- [ ] **Step 4: Tests pass**

`mvn -Dtest=ChatOrchestratorTest test` → PASS

- [ ] **Step 5: Commit**

```bash
git commit -am "feat: add chat orchestrator with routing, rag, and session persistence"
```

---

### Task 7: SSE streaming chat

**Files:**
- Modify: `ModelGateway` / `ModelGatewayService` stream method
- Modify: `ChatOrchestrator` + `ChatController`
- Create: `src/test/java/com/enterprise/csai/chat/ChatStreamContractTest.java` (optional light test)

**Interfaces:**
- Produces: `POST /api/v1/chat/stream` returns `SseEmitter` or `text/event-stream`
- Events: `event:delta` data=token; final `event:meta` data=JSON of route+sources+sessionId

- [ ] **Step 1: Implement stream on gateway**

Use `chatModel.stream(prompt)` and map output text chunks.

- [ ] **Step 2: Controller**

```java
@PostMapping(value = "/api/v1/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter stream(@Valid @RequestBody ChatRequest request) {
    return orchestrator.stream(request);
}
```

Verify response is **not** wrapped into ApiResponse JSON (SSE). If wrapped, exclude streaming return types in a custom `ResponseBodyAdvice` `@ConditionalOnMissingBean` override only if necessary — prefer framework behavior first.

- [ ] **Step 3: Manual or MockMvc smoke**

Document curl:

```bash
curl -N -X POST localhost:8080/api/v1/chat/stream \
  -H 'Content-Type: application/json' \
  -d '{"message":"你好"}'
```

- [ ] **Step 4: Commit**

```bash
git commit -am "feat: add SSE streaming chat endpoint"
```

---

### Task 8: Admin Thymeleaf UI

**Files:**
- Create: `admin/AdminController.java`
- Create: `templates/admin/index.html`, `knowledge.html`, `chat.html`, `models.html`
- Create: `static/admin/app.css`

**Interfaces:**
- Produces pages:
  - `GET /admin` dashboard links
  - `GET/POST /admin/knowledge` upload + list + delete form
  - `GET/POST /admin/chat` form posts message, renders answer/route/sources
  - `GET /admin/models` read-only models + intent mapping

- [ ] **Step 1: AdminController**

Use server-side calls to services (not only JS fetch) for simplicity:

```java
@Controller
@RequestMapping("/admin")
public class AdminController {
    @GetMapping
    public String index() { return "admin/index"; }

    @GetMapping("/knowledge")
    public String knowledge(Model model) { ... }

    @PostMapping("/knowledge/upload")
    public String upload(@RequestParam MultipartFile file, @RequestParam(required=false) String title) { ... }

    @GetMapping("/chat")
    public String chatPage() { return "admin/chat"; }

    @PostMapping("/chat")
    public String chatSubmit(@RequestParam String message, Model model) {
        ChatResponse resp = orchestrator.chat(new ChatRequest(null, message, null));
        model.addAttribute("resp", resp);
        return "admin/chat";
    }

    @GetMapping("/models")
    public String models(Model model) { ... }
}
```

- [ ] **Step 2: Minimal HTML** (Chinese labels OK)

Keep CSS simple; no SPA framework.

- [ ] **Step 3: Manual check**

Start postgres + app; open `/admin/chat`.

- [ ] **Step 4: Commit**

```bash
git commit -am "feat: add thymeleaf admin console for knowledge, chat, models"
```

---

### Task 9: Default model config samples + mock profile + README

**Files:**
- Modify: `application.yml` with two answer models + classifier placeholders
- Modify: `application-mock.yml` to register `ChatModel` fakes via `@Profile("mock")` config
- Create: `src/main/java/com/enterprise/csai/modelgateway/mock/MockModelConfiguration.java`
- Update: `README.md` full runbook
- Update: wire sample env mapping for classifier/strong/fast

**Mock behavior:**
- Classifier always returns PRODUCT JSON for non-greeting; CHITCHAT for 你好/hello
- Answer model returns fixed string including any provided context snippet markers
- Embedding: if Spring AI requires real embedding for vector store tests, use a `EmbeddingModel` bean that returns deterministic unit vectors (dimension 1536) under mock profile **or** skip vector IT under mock

- [ ] **Step 1: Implement mock profile beans**

- [ ] **Step 2: Ensure `mvn test` green without external API keys**

Run: `mvn test`  
Expected: all unit tests PASS; IT skipped or pass with Testcontainers.

- [ ] **Step 3: README sections**

1. Prerequisites (JDK21, Maven, Docker)  
2. Install framework script  
3. `docker compose up -d`  
4. Configure `.env`  
5. `mvn spring-boot:run`  
6. Admin + API examples  
7. Acceptance checklist pointer  

- [ ] **Step 4: Commit**

```bash
git commit -am "docs: complete runbook and mock profile for offline tests"
```

---

### Task 10: End-to-end acceptance pass + doc polish

**Files:**
- Modify: any bugs found
- Optionally mark items in `docs/acceptance/ACCEPTANCE-智能客服-MVP.md` during dry-run (do not fake signatures)

- [ ] **Step 1: Start stack**

```bash
./scripts/install-framework.sh
docker compose up -d
# export env or use .env
mvn spring-boot:run
```

- [ ] **Step 2: Execute P0 checklist manually**

- ping ApiResponse  
- models list  
- upload `samples/refund-policy.md`  
- chat about 退款 → sources nonempty (with real embedding+chat keys)  
- admin pages load  

With only mock profile, document which P0 items require real keys.

- [ ] **Step 3: Final test suite**

```bash
mvn test
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git commit -am "test: close MVP acceptance dry-run fixes"
```

---

## Spec Coverage Checklist (self-review)

| Spec requirement | Task |
|------------------|------|
| Framework parent + starters | Task 1 |
| ApiResponse / RequestId | Task 2 |
| Flyway business tables | Task 2 |
| Multi-model gateway | Task 3 |
| LLM intent routing + degrade | Task 4 |
| Knowledge upload + PgVector RAG | Task 5 |
| Chat sync + session + sources | Task 6 |
| SSE stream | Task 7 |
| Admin UI | Task 8 |
| Mock profile + README | Task 9 |
| Acceptance dry-run | Task 10 |
| Security permit paths | Task 1–2 |
| OpenAI-compatible only | Task 3 |
| Spring AI 1.1.8 / Boot 3.3.13 | Task 1 Global Constraints |

## Placeholder / consistency notes for implementers

- Spring AI 1.1.8 exact builder class names (`getText()` vs `getContent()`, `SearchRequest` builder) must be confirmed against the resolved JAR at compile time; behavior in this plan is normative, method names may need 1-line adjustments.
- If `spring-ai-starter-vector-store-pgvector` auto-config fights custom datasource, set documented `spring.ai.vectorstore.pgvector.*` only; do not fork framework database starter.
- Primary keys: **UUID** (`UUID.randomUUID()`) as decided in design open questions.
- Do not implement Nacos/Apollo/Kafka/Feign in this plan.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-15-intelligent-customer-service-mvp.md`.

**Two execution options:**

1. **Subagent-Driven (recommended)** — dispatch a fresh subagent per task, review between tasks, fast iteration  
2. **Inline Execution** — execute tasks in this session with executing-plans and checkpoints  

**Which approach?**
