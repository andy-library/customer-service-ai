# 全本地 llama.cpp 部署指南（Mac M1 64GB）

面向 **customer-service-ai**：Chat + Embedding 均走本机 `llama-server`（OpenAI 兼容），Postgres+PgVector 仍用 Docker。

## 0. 本机盘点结果

| 项 | 现状 |
|----|------|
| 机器 | Mac M1，统一内存 64GB |
| llama.cpp | Homebrew `llama-server`（已装） |
| 模型目录 | `/Users/andy.yang/LocalModels` |
| 已有对话模型 | **Qwen3.6-35B-A3B**（IQ2_M 11G / Q6 29G / Q8 41G）、Qwythos 9B、Gemma4 31B |
| 已有向量模型 | **无**（需下载） |
| 现有启动器 | `LocalModels/start-mac.sh`（默认 8080，交互式） |

**优先 Qwen：** 客服用 **Qwen3.6-35B-A3B** 做 Chat；Embedding 建议 **Qwen3-Embedding** 或 **bge-m3**（见下）。

---

## 1. 推荐组合（M1 64G）

### 1.1 Chat（已有，无需再下对话大模型）

| 用途 | 推荐文件 | 约占用 | 说明 |
|------|----------|--------|------|
| **默认推荐** | `Qwen3.6-35B-A3B/...-IQ2_M.gguf` | ~11GB + KV | 质量/速度平衡，客服足够 |
| 质量优先 | `...-Q6_K_P.gguf` | ~29GB + KV | 64G 可跑，ctx 建议 ≤16k～32k |
| 不推荐起步 | Q8 41G | 过大 | 留给离线研究 |

路径（默认推荐）：

```text
/Users/andy.yang/LocalModels/Qwen3.6-35B-A3B/Qwen3.6-35B-A3B-Uncensored-HauhauCS-Aggressive-IQ2_M.gguf
```

- **端口：** `8082`  
- **API model 名（alias）：** `qwen3.6-35b`  
- **ctx：** 客服+RAG 建议 `16384`（内存紧用 `8192`）

### 1.2 Embedding（需下载）

本地**没有**专用向量 GGUF，RAG 必须另下。按「优先 Qwen 系」：

| 优先级 | 模型 | 建议 GGUF 来源（Hugging Face） | 典型维度 |
|--------|------|--------------------------------|----------|
| **1（推荐）** | **Qwen3-Embedding-0.6B** | 搜索 `Qwen3-Embedding-0.6B` + `GGUF`（如 `Qwen/Qwen3-Embedding-0.6B-GGUF` 或社区量化仓） | 常见 **1024**（以文件/实测为准） |
| 2 | bge-m3 | `CompendiumLabs/bge-m3-gguf` 等 | 1024 |
| 3 | bge-small-zh-v1.5 | 更小更快 | 512 |

**下载示例（任选其一工具）：**

```bash
# 需要安装 huggingface-cli: pip install -U "huggingface_hub[cli]"
mkdir -p /Users/andy.yang/LocalModels/Qwen3-Embedding-0.6B
cd /Users/andy.yang/LocalModels/Qwen3-Embedding-0.6B

# 示例：按仓库实际文件名调整（下载前在 HF 页面确认 .gguf 文件名）
huggingface-cli download Qwen/Qwen3-Embedding-0.6B-GGUF \
  --include "*.gguf" \
  --local-dir .

# 若该仓库结构不同，可手动从浏览器下载 Q4_K_M / Q8_0 等到上述目录
```

备用 bge-m3：

```bash
mkdir -p /Users/andy.yang/LocalModels/bge-m3
cd /Users/andy.yang/LocalModels/bge-m3
huggingface-cli download CompendiumLabs/bge-m3-gguf \
  --include "*Q4*.gguf" \
  --local-dir .
```

- **端口：** `8083`  
- **API model 名：** `qwen3-embedding` 或 `bge-m3`  
- 启动参数必须带：`--embedding`

### 1.3 拓扑

```
Admin / API :8081  (Spring customer-service-ai)
    ├─ Chat  ──────► llama-server :8082  (Qwen3.6-35B-A3B)
    └─ Embed ──────► llama-server :8083  (Qwen3-Embedding / bge-m3)
                           │
                    Postgres+pgvector :5432
```

不要与 CC Switch `15721`、本机占用的 `8080` 冲突。

---

## 2. 一键脚本（本仓库已提供）

路径：`scripts/local-llm/`

| 脚本 | 作用 |
|------|------|
| `start-chat.sh` | 启动 Qwen Chat @8082 |
| `start-embed.sh` | 启动 Embedding @8083（需模型文件存在） |
| `stop-all.sh` | 停止 8082/8083 |
| `healthcheck.sh` | curl models / 试探 embeddings |

先：

```bash
chmod +x scripts/local-llm/*.sh
```

---

## 3. 完整操作步骤

### Step 1 — 确认 Postgres

```bash
cd "/Volumes/Development HD/SourceCode/Spring AI"
export DOCKER_HOST=unix://$HOME/.rd/docker.sock   # Rancher Desktop 时
docker compose up -d postgres
```

### Step 2 — 下载 Embedding 模型（仅首次）

见 §1.2。下载完成后，编辑 `scripts/local-llm/start-embed.sh` 中的 `MODEL` 路径（脚本内已有注释占位）。

### Step 3 — 启动本地 LLM

```bash
# 终端 A：Chat
./scripts/local-llm/start-chat.sh

# 终端 B：Embedding（模型就绪后）
./scripts/local-llm/start-embed.sh

# 健康检查
./scripts/local-llm/healthcheck.sh
```

期望：

- `http://127.0.0.1:8082/v1/models` 含 alias `qwen3.6-35b`  
- `http://127.0.0.1:8083/v1/embeddings` 对测试文本返回向量数组  

### Step 4 — 配置客服系统 `.env`（全本地）

在项目根 `.env` 中设为（**不要**再用百炼 URL；Key 可用占位）：

```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=csai
DB_USER=csai
DB_PASSWORD=csai

CS_AI_DEFAULT_BASE_URL=http://127.0.0.1:8082/v1
CS_AI_DEFAULT_API_KEY=sk-local

CS_AI_CLASSIFIER_MODEL=qwen3.6-35b
CS_AI_ANSWER_STRONG_MODEL=qwen3.6-35b
CS_AI_ANSWER_FAST_MODEL=qwen3.6-35b

CS_AI_EMBEDDING_BASE_URL=http://127.0.0.1:8083/v1
CS_AI_EMBEDDING_API_KEY=sk-local
CS_AI_EMBEDDING_MODEL=qwen3-embedding
# 维度以实测为准：curl embeddings 后看 length；Qwen3-Embedding-0.6B 多为 1024
CS_AI_EMBEDDING_DIMENSIONS=1024

CS_AI_WORKER_ID=1
SERVER_PORT=8081
```

若实测维度不是 1024：

```bash
# 看返回 embedding 数组长度
python3 - <<'PY'
import json,urllib.request
req=urllib.request.Request(
  "http://127.0.0.1:8083/v1/embeddings",
  data=json.dumps({"model":"qwen3-embedding","input":"test"}).encode(),
  headers={"Content-Type":"application/json"})
print(len(json.load(urllib.request.urlopen(req))["data"][0]["embedding"]))
PY
```

把输出写回 `CS_AI_EMBEDDING_DIMENSIONS`。若从云端 1024/1536 改过维度，重建向量表：

```bash
export DOCKER_HOST=unix://$HOME/.rd/docker.sock
docker exec -it springai-postgres-1 psql -U csai -d csai -c 'DROP TABLE IF EXISTS vector_store;'
```

重启 Spring 后会按新维度初始化。

### Step 5 — 启动客服应用（非 mock）

```bash
cd "/Volumes/Development HD/SourceCode/Spring AI"
set -a && source .env && set +a
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### Step 6 — 验收

```bash
./scripts/smoke-real.sh http://localhost:8081
# 或浏览器
open http://localhost:8081/admin
```

| 检查 | 期望 |
|------|------|
| ping | code=0 |
| models | 三个角色 modelName=qwen3.6-35b |
| 上传 refund-policy | INDEXED |
| 「你好」 | 中文回复 |
| 「如何申请退款」 | 有 answer，尽量有 sources |

---

## 4. 与现有 LocalModels 启动器的关系

| | `LocalModels/start-mac.sh` | 本仓库 `scripts/local-llm/*` |
|--|---------------------------|------------------------------|
| 用途 | 交互选模型，默认 **8080** | 固定给客服系统，**8082/8083** |
| 是否冲突 | 8080 常被占用 | 避开 8080/8081 |
| 是否必须停掉 8080 | 建议停，避免混淆 | Chat 只认 8082 |

可继续用 `start-mac.sh` 做日常聊天；**客服系统请用本仓库脚本**，端口分离。

---

## 5. M1 性能提示

| 参数 | 建议 |
|------|------|
| `-ngl 99` | Metal 尽量卸层到 GPU |
| `-c 8192~16384` | RAG 够用；过大占统一内存 |
| `-np 1` | 客服演示单会话优先稳 |
| Chat 量化 | 演示用 **IQ2_M**；要更好回答再换 **Q6** |
| 首次加载 | Qwen 大模型冷启动可能 30s～数分钟，属正常 |

---

## 6. 故障排查

| 现象 | 处理 |
|------|------|
| Spring 调 Chat 404 | base 用 `http://127.0.0.1:8082/v1`；本项目会自动用 `/chat/completions` |
| embeddings 空/错 | 确认 `--embedding` 且模型是 embedding GGUF，不是对话模型 |
| Model access / connection refused | 先 `healthcheck.sh`，确认 8082/8083 LISTEN |
| 向量维度错误 | 改 DIMENSIONS + DROP vector_store |
| 回答慢 | 降 ctx、用 IQ2、关其它占内存 App |
| 意图 JSON 乱 | 分类与回答共用 Qwen 一般可；可加温度限制（后续可在代码里设 temperature） |

---

## 7. 推荐下载清单（总结）

| 已有 | 动作 |
|------|------|
| Qwen3.6-35B-A3B IQ2_M / Q6 | **直接用作 Chat** |
| Embedding | **下载 Qwen3-Embedding-0.6B GGUF**（首选）或 bge-m3 GGUF |
| llama-server | 已装，无需再装 |

---

## 8. 安全

- 本地 `sk-local` 仅本机；勿把真实云 Key 写进脚本  
- `.env` 不提交 Git  
- 仅监听 `127.0.0.1`，不要默认 `0.0.0.0` 暴露到局域网（除非你有意）  
