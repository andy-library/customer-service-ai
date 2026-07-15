# 阿里云百炼 GLM-5.1 联调配置（客服系统）

> 不含 API Key。Key 请只写在本地 `.env`，勿提交仓库。

## 1. 协议差异（必读）

| 端点 | 协议 | 本项目 Spring AI（OpenAI 客户端）能否直连 |
|------|------|------------------------------------------|
| `https://dashscope.aliyuncs.com/apps/anthropic` | **Anthropic** Messages API | ❌ 不能直连 |
| `https://dashscope.aliyuncs.com/compatible-mode/v1` | **OpenAI** Chat Completions | ✅ 推荐直连 |
| CC Switch 本地代理 `http://127.0.0.1:15721` | 主要为 Claude Code / Codex 热切换与协议转换 | ⚠️ 对本 Spring 应用不是默认路径（见下文） |

你在 CC Switch 里当前 Claude 供应商 **Ali-GLM 5.1** 用的是：

- `ANTHROPIC_BASE_URL=https://dashscope.aliyuncs.com/apps/anthropic`
- `ANTHROPIC_MODEL=glm-5.1`（以及 Haiku/Opus/Sonnet 默认都映射到 `glm-5.1`）

这与 **Claude Code** 匹配；本客服系统要用 **OpenAI 兼容端点** 调同一个百炼 Key / 同一模型名 `glm-5.1`。

---

## 2. 推荐方案：Spring 应用直连百炼 OpenAI 兼容（glm-5.1）

### 2.1 `.env`（除 API Key 外全部可照抄）

```bash
# ---- 数据库（本地 Compose）----
DB_HOST=localhost
DB_PORT=5432
DB_NAME=csai
DB_USER=csai
DB_PASSWORD=csai

# ---- 百炼 OpenAI 兼容（华北2 北京）----
# 注意：不是 /apps/anthropic
CS_AI_DEFAULT_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
CS_AI_DEFAULT_API_KEY=   # 只填你的百炼 API Key

# ---- Chat：全部使用 glm-5.1（你当前选用）----
CS_AI_CLASSIFIER_BASE_URL=
CS_AI_CLASSIFIER_API_KEY=
CS_AI_CLASSIFIER_MODEL=glm-5.1

CS_AI_ANSWER_STRONG_BASE_URL=
CS_AI_ANSWER_STRONG_API_KEY=
CS_AI_ANSWER_STRONG_MODEL=glm-5.1

CS_AI_ANSWER_FAST_BASE_URL=
CS_AI_ANSWER_FAST_API_KEY=
CS_AI_ANSWER_FAST_MODEL=glm-5.1

# ---- Embedding（RAG 必需；与 Chat 模型分离）----
# 百炼推荐用通义文本向量模型（OpenAI 兼容 embeddings 接口）
CS_AI_EMBEDDING_BASE_URL=
CS_AI_EMBEDDING_API_KEY=
CS_AI_EMBEDDING_MODEL=text-embedding-v3
# text-embedding-v3 常用维度 1024；若控制台/文档为 1536 请改一致
CS_AI_EMBEDDING_DIMENSIONS=1024

# ---- 框架 ----
CS_AI_WORKER_ID=1
SERVER_PORT=8081
```

说明：

- `CS_AI_*_BASE_URL` / `*_API_KEY` 留空时，会回落到 `CS_AI_DEFAULT_*`（同一 Key、同一域名即可）。
- 分类 / 强答 / 快答目前都可先指向 **同一个 `glm-5.1`**；以后若要成本分层，再把 FAST 换成更小的百炼模型即可。
- **glm-5.1 不做 embedding**；RAG 必须另配 `text-embedding-v3`（或 `text-embedding-v4` 等，以控制台为准）。

### 2.2 对应 `application.yml` 语义（无需改代码即可）

| 配置项 | 值 |
|--------|-----|
| Chat base-url | `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| Chat model | `glm-5.1` |
| Embedding model | `text-embedding-v3` |
| Vector dimensions | `1024`（与 embedding 模型一致） |
| Profile | **不要** 使用 `mock` |
| 端口 | 建议 `8081`（本机 8080 常被其他进程占用） |

### 2.3 启动

```bash
cp .env.example .env   # 若还没有
# 按上面填写 .env（URL/模型已给全，只补 Key）

./scripts/check-env.sh
./scripts/run-real.sh
# 另开终端
./scripts/smoke-real.sh http://localhost:8081
```

### 2.4 与你提供的 Anthropic URL 的关系

| 场景 | Base URL | 模型 |
|------|----------|------|
| Claude Code + CC Switch（你已在用） | `https://dashscope.aliyuncs.com/apps/anthropic` | `glm-5.1` |
| **本客服系统 Spring AI** | `https://dashscope.aliyuncs.com/compatible-mode/v1` | `glm-5.1` |

**同一把百炼 API Key** 通常两种协议都能用；变的是 **Base URL + 协议**，不是模型名。

---

## 3. CC Switch 如何配合本项目

### 3.1 CC Switch 在本机的角色

根据本机 `~/.cc-switch`：

| 项 | 值 |
|----|-----|
| 本地代理 | 已开启，`127.0.0.1:15721` |
| Claude 当前供应商 | **Ali-GLM 5.1**（Anthropic 协议 → 百炼 `/apps/anthropic`） |
| OpenCode 供应商「阿里云百炼」 | 已有 OpenAI 兼容：`compatible-mode/v1` + `glm-5.1` |

CC Switch 擅长：

1. 给 **Claude Code / Codex / Gemini CLI** 一键切换供应商与模型  
2. 本地代理做协议转换、故障切换  

它**不是**本 Spring 应用默认依赖的 OpenAI 网关。实测对本机代理发 `/v1/chat/completions` 会按 Codex 链路处理，当前 Codex 默认供应商还缺 `base_url`，不适合直接给客服系统用。

### 3.2 推荐配合方式（两套配置并行）

```
┌─────────────────────┐     Anthropic 协议      ┌──────────────────────────────┐
│ Claude Code         │ ──────────────────────► │ dashscope .../apps/anthropic │
│ (经 CC Switch)      │   glm-5.1               │ 百炼                         │
└─────────────────────┘                         └──────────────────────────────┘

┌─────────────────────┐     OpenAI 协议         ┌──────────────────────────────┐
│ customer-service-ai │ ──────────────────────► │ dashscope .../compatible-mode/v1 │
│ (Spring AI)         │   glm-5.1 + embedding   │ 百炼                         │
└─────────────────────┘                         └──────────────────────────────┘
```

- **CC Switch**：继续维护 Claude 侧 `Ali-GLM 5.1`（你已配好的 Anthropic URL）。  
- **客服系统**：`.env` 用 OpenAI 兼容 URL + 同一 Key + `glm-5.1`。  
- 切换模型时：  
  - 在 CC Switch 改 Claude 默认模型（Haiku/Sonnet/Opus → 其它百炼模型）  
  - 在客服系统改 `.env` 的 `CS_AI_*_MODEL`（或以后做成管理台配置）

### 3.3 若坚持「流量只走本机 CC Switch」

当前本地代理更适合 CLI 热切换，**不建议**作为本 MVP 的默认 Chat 入口。若后续要统一出口，更稳妥的是：

1. 在 CC Switch 或旁路再挂一个 **OpenAI 兼容** 本地转发（如 one-api / new-api / 自建网关），Base URL 形如 `http://127.0.0.1:xxxx/v1`；或  
2. 本项目增加 Anthropic 客户端（额外开发），直连 `.../apps/anthropic`。

MVP 范围内 **优先直连 `compatible-mode/v1`**。

### 3.4 CC Switch 侧建议核对（非本仓库文件）

Claude 供应商 **Ali-GLM 5.1**（你已接近正确）：

| 项 | 建议值 |
|----|--------|
| 协议 | Anthropic / Claude |
| Base URL | `https://dashscope.aliyuncs.com/apps/anthropic` |
| Model | `glm-5.1` |
| API Key | 百炼 Key（与 Spring `.env` 可相同） |

OpenCode / 其它 OpenAI 协议工具（可选）：

| 项 | 建议值 |
|----|--------|
| Base URL | `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| Model | `glm-5.1` |

---

## 4. 多模型选择（同一 Key）

百炼控制台可开多个模型；本系统通过 `csai.models` 注册多条，例如：

| 角色 | 建议 model | 用途 |
|------|------------|------|
| CLASSIFIER | `glm-5.1` 或更小更快模型 | 意图分类（成本/时延敏感时可换小模型） |
| ANSWER（strong） | `glm-5.1` | 复杂售后/政策 |
| ANSWER（fast） | `glm-5.1`（当前）或更小模型 | 闲聊 |
| EMBEDDING | `text-embedding-v3` | RAG 向量 |

切换时只改 `.env` 中 `CS_AI_*_MODEL`，无需改代码。

---

## 5. 联调检查清单

1. [ ] `.env` 使用 **compatible-mode/v1**，不是 `/apps/anthropic`  
2. [ ] Chat 模型名为 **`glm-5.1`**  
3. [ ] Embedding 已配置且 **dimensions 与模型一致**  
4. [ ] 启动时 **未** 带 `mock` profile  
5. [ ] `./scripts/smoke-real.sh`：上传退款文档后提问能返回 sources（视检索阈值）  
6. [ ] CC Switch 仍用于 Claude Code，不与 Spring base-url 混用 Anthropic 地址  

---

## 6. 常见错误

| 现象 | 原因 | 处理 |
|------|------|------|
| 404 / path not found | 把 Anthropic URL 当 OpenAI 用 | 改成 `compatible-mode/v1` |
| 401 | Key 错误或未开通模型 | 控制台开通 GLM / Embedding |
| 向量维度错误 | dimensions 与 embedding 模型不符 | 改 `CS_AI_EMBEDDING_DIMENSIONS` 并重建向量表 |
| 本地 15721 调 /chat/completions 失败 | CC Switch 代理按 Codex/Claude 路由 | Spring 直连百炼兼容端点 |


## 7. 联调实测记录（2026-07-15）

| 能力 | 结果 |
|------|------|
| Base URL `compatible-mode/v1` + 路径去重 `/v1` | 已修复（OpenAiCompatibleClients） |
| Chat `glm-5.1` 闲聊 | ✅ 成功（intent=CHITCHAT） |
| Embedding `text-embedding-v3` | ❌ `Model.AccessDenied`（控制台需开通/授权） |
| RAG 上传/检索 | 依赖 embedding 开通后重试 |

**处理：** 在百炼控制台开通 **text-embedding-v3**（或改用你有权限的 embedding 模型，并同步改 `.env` 的 `CS_AI_EMBEDDING_MODEL` / `CS_AI_EMBEDDING_DIMENSIONS`）。
