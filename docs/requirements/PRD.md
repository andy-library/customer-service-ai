# 产品需求说明 — customer-service-ai

| 字段 | 值 |
|------|-----|
| 版本 | **1.0**（对应应用 `0.2.0-rc.1`） |
| 状态 | **已实现** |
| 作者 | andy yang |
| 范围 | 准生产智能客服编排 |

---

## 1. 目标

提供基于 Spring AI 的**客服编排服务**，实现：

- 意图分类并路由到合适模型  
- 检索企业知识（Dify 主路径；可选本地 PgVector）  
- 生成可引用 **sources** 的回答  
- 支持 **local**（llama.cpp）与 **cloud**（OpenAI 兼容）模型后端  
- 可选 API Key 安全、限流、超时、降级标记与转人工占位  
- 暴露健康检查、指标、审计与 Docker 交付  

### 1.1 成功标准

| ID | 标准 |
|----|------|
| G1 | 开启安全时未授权访问被拒绝（401） |
| G2 | 会话归属跨主体强制校验 |
| G3 | 模型/Dify 超时与知识降级路径可用 |
| G4 | 超限返回 429 |
| G5 | 响应含 `route`、`sources`，以及可选 `degraded` / `handoff` |
| G6 | 知识提供方：`dify` \| `local` \| `none` |
| G7 | 转人工占位（低置信 / 用户请求 / UNKNOWN 策略） |
| G8 | Docker + Runbook + 冒烟脚本齐全 |
| G9 | 自动化单元测试通过（`mvn test`） |

### 1.2 非目标

- 完整工单 / CRM  
- 多渠道中台（企微、钉钉等）  
- SaaS 多租户计费  
- 微服务网格拆分  
- 用本服务替代 Dify 做知识创作平台  

---

## 2. 角色

| 角色 | 诉求 |
|------|------|
| 终端用户（经 BFF/客户端） | 提问并获得有据回答 |
| 运营 | 管理台对话测试、知识状态、模型列表 |
| 研发 / SRE | 配置模型与 Dify、安全策略，观察健康与指标 |

---

## 3. 功能需求（已实现）

### 对话

- `POST /api/v1/chat` — 同步回答  
- `POST /api/v1/chat/stream` — SSE 流式（`status` / `delta` / `meta`）  
- 通过 `sessionId` 续聊 + 服务端历史  
- 响应字段：`answer`、`route`、`sources`、`degraded`、`degradedReasons`、`handoff`、`handoffReason`  

### 知识

- `dify`：Dataset retrieve + retrieve 失败时的 segment 降级  
- `local`：PgVector 入库与检索  
- `none`：不检索（如 mock）  

### 模型

- `csai.model-source=local|cloud`  
- 角色：classifier、answer-strong、answer-fast  
- 分类 / 回答 / Dify HTTP 超时  

### 安全与韧性

- 可选 API Key（`X-API-Key` / Bearer），`/admin/**` 需 ADMIN  
- 滑动窗口限流  
- 护栏：系统提示、依据策略、基础注入特征  
- 转人工策略  

### 运维

- Flyway + Schema 保障（归属/审计字段）  
- Micrometer（`csai.*`）  
- 健康指示器  
- Admin Thymeleaf（流式对话台）  
- Dockerfile、Compose、冒烟脚本  

---

## 4. API 摘要

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/ping` | 公开 |
| GET | `/api/v1/runtime` | 当前模型/知识源 |
| POST | `/api/v1/chat` | 同步 |
| POST | `/api/v1/chat/stream` | SSE |
| POST | `/api/v1/knowledge/search` | 检索片段 |
| GET | `/admin/**` | 运维 UI（开安全时需 ADMIN） |
| GET | `/actuator/health` | 健康 |

业务 JSON 接口由框架包装为 `ApiResponse`（`code` / `message` / `data`）。SSE **不**包装。

---

## 5. 配置（详见 configuration.md）

关键开关：

- `CS_AI_MODEL_SOURCE` — `local` \| `cloud`  
- `CS_AI_KNOWLEDGE_PROVIDER` — `dify` \| `local` \| `none`  
- `CSAI_SECURITY_ENABLED` — API Key 强制  
- `DIFY_BASE_URL` / `DIFY_API_KEY` / `DIFY_DATASET_ID`  

---

## 6. 验收

见 [../acceptance/ACCEPTANCE.md](../acceptance/ACCEPTANCE.md)。
