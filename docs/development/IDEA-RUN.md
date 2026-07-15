# IntelliJ IDEA 启动配置

Author: **andy yang**

本文说明如何在本地 IntelliJ IDEA 中启动 **customer-service-ai** 做开发验证。

仓库已提供可共享运行配置（目录 [`.run/`](../../.run/)），用 IDEA 打开项目后应出现在 **Run/Debug Configurations**。

---

## 1. 一次性准备

### 1.1 导入项目

1. **File → Open** 选择仓库根目录（含 `pom.xml`）
2. 选择 **Open as Maven Project** / 信任项目
3. 等待 Maven 依赖下载完成  
4. **Project Structure → Project SDK** 选择 **JDK 21**
5. 若 parent 解析失败，先执行：

```bash
./scripts/install-framework.sh
```

然后在 IDEA 右侧 **Maven → Reload**。

### 1.2 启动 PostgreSQL

```bash
docker compose up -d postgres
```

### 1.3（可选）真实模型依赖

| 依赖 | 用途 | 说明 |
|------|------|------|
| llama.cpp Chat `:18080` | 分类 + 回答 | `CS_AI_MODEL_SOURCE=local` |
| Dify Dataset API | 知识检索 | `CS_AI_KNOWLEDGE_PROVIDER=dify` |
| llama Embed `:18081` | 仅 `knowledge=local` | Dify 主路径可不启 |

---

## 2. 仓库内置 Run Configuration

打开 **Run → Edit Configurations**，应看到：

| 配置名称 | 类型 | 场景 |
|----------|------|------|
| **Csai · Mock (offline)** | Spring Boot | 无外部 LLM/Dify，单元/界面冒烟 |
| **Csai · Local Real (llama + Dify)** | Spring Boot | 本地联调真实模型与知识库 |
| **Csai · Application Mock** | Application | Community 版无 Spring Boot 运行类型时用 |
| **Csai · Application Local** | Application | 同上，本地真实依赖 |

> 若列表没有配置：确认已打开正确根目录；或 **Run → Edit Configurations → +** 按下文手建。

主类统一为：

```text
com.enterprise.csai.CustomerServiceAiApplication
```

Module / Use classpath of module：

```text
customer-service-ai
```

Program arguments（推荐）：

```text
--server.port=8081
```

VM options（推荐）：

```text
-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai
```

Working directory：

```text
$PROJECT_DIR$
```

---

## 3. 推荐：Mock 配置（最快验证）

适合：**只验证启动、Admin、API 契约、无密钥环境**。

| 项 | 值 |
|----|-----|
| Main class | `com.enterprise.csai.CustomerServiceAiApplication` |
| Active profiles | `mock` |
| Program arguments | `--server.port=8081` |
| 外部依赖 | 仅需 PostgreSQL |

启动后：

- Health: http://localhost:8081/actuator/health  
- Admin: http://localhost:8081/admin  
- Ping: http://localhost:8081/api/v1/ping  

---

## 4. 本地真实联调配置

### 4.1 环境变量

在 Run Configuration → **Environment variables** 中配置（或使用 **EnvFile** 插件加载项目根目录 `.env`）：

```properties
DB_HOST=localhost
DB_PORT=5432
DB_NAME=csai
DB_USER=csai
DB_PASSWORD=csai

CS_AI_MODEL_SOURCE=local
CS_AI_KNOWLEDGE_PROVIDER=dify
CS_AI_DEFAULT_BASE_URL=http://127.0.0.1:18080/v1
CS_AI_DEFAULT_API_KEY=sk-local
CS_AI_CLASSIFIER_MODEL=local-qwen
CS_AI_ANSWER_STRONG_MODEL=local-qwen
CS_AI_ANSWER_FAST_MODEL=local-qwen
CS_AI_EMBEDDING_BASE_URL=http://127.0.0.1:18081/v1
CS_AI_EMBEDDING_API_KEY=sk-local
CS_AI_EMBEDDING_MODEL=local-bge-m3
CS_AI_EMBEDDING_DIMENSIONS=1024

DIFY_BASE_URL=http://127.0.0.1:15001/v1
DIFY_API_KEY=<你的 Dataset API Key>
DIFY_DATASET_ID=<你的 Dataset ID>
DIFY_SEARCH_METHOD=semantic_search

CSAI_SECURITY_ENABLED=false
SERVER_PORT=8081
```

**不要把真实 Key 写进可提交的 `.run/*.xml`。** 只在本机 Run Configuration 或本地 `.env`（已 gitignore）中填写。

### 4.2 加载 `.env` 的两种方式

**方式 A — EnvFile 插件（推荐）**

1. 安装插件 **EnvFile**
2. Run Configuration → **EnvFile** 勾选 Enable  
3. 添加 `$PROJECT_DIR$/.env`

**方式 B — 手填 Environment variables**

将上表粘贴到 **Environment variables** 文本框（`KEY=value;` 格式也可）。

### 4.3 开启鉴权做安全验证时

```properties
CSAI_SECURITY_ENABLED=true
CSAI_API_KEY_CLIENT=local-dev-client
CSAI_API_KEY_ADMIN=local-dev-admin
```

请求时加头：

```http
X-API-Key: local-dev-client
```

---

## 4. 手建 Spring Boot 配置（Ultimate）

1. **Run → Edit Configurations → + → Spring Boot**
2. 填写：

| 字段 | 值 |
|------|-----|
| Name | `Csai Local` |
| Main class | `com.enterprise.csai.CustomerServiceAiApplication` |
| Module | `customer-service-ai` |
| Active profiles | 空 或 `mock` |
| Program arguments | `--server.port=8081` |
| Working directory | `$PROJECT_DIR$` |
| JRE | 21 |

3. Environment variables 按上文填写  
4. Apply → Run  

---

## 5. 手建 Application 配置（Community）

1. **+ → Application**
2. Main class / Module 同上  
3. Program arguments：

```text
--spring.profiles.active=mock --server.port=8081
```

或真实联调时不设 profile，改用 Environment variables。

---

## 6. 启动后自检

```bash
# 健康
curl -s http://127.0.0.1:8081/actuator/health | jq

# 运行时（模型源 / 知识库）
curl -s http://127.0.0.1:8081/api/v1/runtime | jq

# 对话（security=false）
curl -s -X POST http://127.0.0.1:8081/api/v1/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"你好"}' | jq
```

浏览器：

- http://localhost:8081/admin  
- http://localhost:8081/swagger-ui.html  

---

## 7. 常见问题

| 现象 | 处理 |
|------|------|
| 找不到 parent POM | `./scripts/install-framework.sh` 后 Maven Reload |
| 端口 8081 占用 | 改 `--server.port=8082` 或结束占用进程 |
| 模块名不对 | Maven 工具窗 Reload；模块名应为 `customer-service-ai` |
| DB 连接失败 | `docker compose up -d postgres`；检查 5432 |
| 分类/回答超时 | 本地大模型较慢，可在 env 加 `CSAI_CLASSIFIER_TIMEOUT_MS=90000` |
| Dify 401/空命中 | 检查 `DIFY_API_KEY` / `DIFY_DATASET_ID` / port-forward |
| 修改 yml 不生效 | Restart（非热更配置 bean） |

---

## 8. Debug 建议

- 断点：`ChatOrchestrator.chat`、`RoutingService.route`、`DifyKnowledgeRetriever.searchDetailed`
- Debug 配置：复制上述 Run Configuration → **Debug**
- 日志：`application-mock.yml` 中 `logging.level.com.enterprise.csai=DEBUG`

---

## 9. 与 Maven 命令对照

| IDEA 配置 | 等价命令 |
|-----------|----------|
| Mock | `mvn spring-boot:run -Dspring-boot.run.profiles=mock -Dspring-boot.run.arguments=--server.port=8081` |
| Local Real | `set -a && source .env && set +a && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` |
