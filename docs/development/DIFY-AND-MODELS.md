# Dify 知识库 + 可插拔本地/云端模型

作者：**andy yang**


## 1. 本机实测（llama.cpp）

| 服务 | 进程参数（摘要） | 端口 | alias | 用途 |
|------|------------------|------|-------|------|
| Chat | Qwen3.6-35B-A3B Q6_K_P, ctx 32768, ngl 99 | **18080** | **local-qwen** | 分类 + 回答 |
| Embedding | bge-m3-FP16, embedding, pooling mean | **18081** | **local-bge-m3** | 仅当 knowledge=local 时用 |
| 向量维度 | bge-m3 实测 | **1024** | | |

自检：

```bash
curl -s http://127.0.0.1:18080/v1/models | head
curl -s http://127.0.0.1:18081/v1/embeddings -H 'Content-Type: application/json' \
  -d '{"model":"local-bge-m3","input":"测试"}'
```

另有 `8080` 上 Qwythos 旧实例，**客服系统不要用 8080**。

---

## 2. 目标架构

```
用户 ──► Spring AI 客服应用 (编排 / 路由 / 会话 / 管理台)
              │
              ├─ ModelGateway ──► [local] llama :18080  (local-qwen)
              │                └► [cloud] 百炼 compatible-mode (glm-5.1 等)
              │
              └─ KnowledgeRetriever
                     ├─ [dify]  Dify Dataset Retrieve API  ← 企业知识库主路径
                     ├─ [local] 可选：本机 embedding + PgVector
                     └─ [none]  不检索
```

**职责划分**

| 组件 | 职责 |
|------|------|
| **Dify** | 知识沉淀、文档切分、索引、检索（主知识库） |
| **Spring AI** | 意图路由、多模型网关、会话、拼装 prompt、调 Dify 检索、生成回答 |
| **llama.cpp** | 本地 OpenAI 兼容 Chat（及可选本地 embedding） |
| **百炼等云端** | 可插拔 Chat（及可选云 embedding）；配置切换即可 |

---

## 3. 配置开关

| 配置 | 值 | 含义 |
|------|-----|------|
| `csai.model-source` | `local` / `cloud` | 对话模型走本地还是云端 |
| `csai.knowledge.provider` | `dify` / `local` / `none` | 知识检索来源 |

环境变量：

```bash
CS_AI_MODEL_SOURCE=local          # 或 cloud
CS_AI_KNOWLEDGE_PROVIDER=dify     # 推荐 dify

# 本地 Chat / Embed
CS_AI_DEFAULT_BASE_URL=http://127.0.0.1:18080/v1
CS_AI_DEFAULT_API_KEY=sk-local
CS_AI_CLASSIFIER_MODEL=local-qwen
CS_AI_ANSWER_STRONG_MODEL=local-qwen
CS_AI_ANSWER_FAST_MODEL=local-qwen
CS_AI_EMBEDDING_BASE_URL=http://127.0.0.1:18081/v1
CS_AI_EMBEDDING_MODEL=local-bge-m3
CS_AI_EMBEDDING_DIMENSIONS=1024

# 云端（model-source=cloud 时使用）
CS_AI_CLOUD_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
CS_AI_CLOUD_API_KEY=
CS_AI_CLOUD_CLASSIFIER_MODEL=glm-5.1
CS_AI_CLOUD_ANSWER_STRONG_MODEL=glm-5.1
CS_AI_CLOUD_ANSWER_FAST_MODEL=glm-5.1
CS_AI_CLOUD_EMBEDDING_BASE_URL=${CS_AI_CLOUD_BASE_URL}
CS_AI_CLOUD_EMBEDDING_MODEL=text-embedding-v3
CS_AI_CLOUD_EMBEDDING_DIMENSIONS=1024

# Dify 知识库
DIFY_BASE_URL=http://127.0.0.1/v1   # 自建或云端 API 前缀，以实际为准
DIFY_API_KEY=                        # Dataset API Key
DIFY_DATASET_ID=                     # 知识库 ID
```

切换本地/云端：只改 **`CS_AI_MODEL_SOURCE=local|cloud`** 并保证对应 Key/地址有效，**重启应用**即可（Registry 在启动时加载）。

---

## 4. Dify 侧要求

1. 在 Dify 创建知识库并完成文档导入/索引。  
2. 创建 **Dataset API Key**（知识库检索权限）。  
3. 复制 **Dataset ID**。  
4. 若自建 Dify，保证 Spring 能访问 `DIFY_BASE_URL`（常见 `http://host/v1`）。  

Spring 调用（实现内置）：

`POST {DIFY_BASE_URL}/datasets/{dataset_id}/retrieve`

---

## 5. 运行顺序

```bash
# 1) 本地 llama（你已启动）
# Chat  :18080 local-qwen
# Embed :18081 local-bge-m3  （仅 knowledge=local 时需要）

# 2) Dify 已部署并完成知识库

# 3) Postgres（会话/路由日志；Dify 模式下可不依赖 vector 入库）
docker compose up -d postgres

# 4) .env 按上文填写后
set -a && source .env && set +a
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

---

## 6. Admin 行为

| knowledge.provider | 管理台知识页 |
|--------------------|--------------|
| dify | 展示 Dify 配置状态与检索调试，**不做本地上传** |
| local | 保留本地上传/列表（PgVector） |
| none | 提示未启用知识库 |

---

## 7. 与旧方案差异

| 旧 | 新 |
|----|-----|
| 知识入库在 Spring + PgVector | **主路径 Dify**；local 为可选 |
| 模型单一 .env | **model-source 本地/云端可切换** |
| 固定 8082/8083 | 对齐你本机 **18080/18081** |

---

## 8. 代码落点（实现索引）

| 能力 | 类 / 配置 |
|------|-----------|
| 模型 local/cloud 解析 | `ActiveModelProfileResolver` |
| Chat 注册 | `ModelGatewayConfiguration` + `csai.models` / `csai.cloud` |
| Embedding | `EmbeddingModelConfiguration`（跟随 model-source） |
| 知识检索 SPI | `KnowledgeRetriever` |
| Dify | `DifyKnowledgeRetriever` → `POST .../datasets/{id}/retrieve` |
| 本地向量 | `LocalVectorKnowledgeRetriever` + `DocumentIngestService`（`provider=local`） |
| 关闭检索 | `NoOpKnowledgeRetriever`（`provider=none` / mock） |
| 运行时视图 | `GET /api/v1/runtime`、`GET /api/v1/knowledge/status` |
| Admin | `/admin/knowledge` 检索调试（dify）或上传（local） |

## 9. 联调检查清单

- [ ] `curl :18080/v1/models` → `local-qwen`
- [ ] （可选）`curl :18081/v1/embeddings` → dim 1024
- [ ] Dify 知识库已索引，Dataset API Key + Dataset ID 已写入 `.env`
- [ ] `./scripts/check-env.sh` PASS
- [ ] `GET /api/v1/runtime` → `modelSource=local`, `knowledgeProvider=dify`
- [ ] Admin 知识库页检索有命中
- [ ] `POST /api/v1/chat` 业务问题返回 `sources`
- [ ] （可选）`CS_AI_MODEL_SOURCE=cloud` 重启后 registry 为 cloud 模型
