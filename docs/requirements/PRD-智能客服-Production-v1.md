# 产品需求文档 — 企业智能客服 Production v1（P0+P1）

| 属性 | 值 |
|------|-----|
| 版本 | **1.0-prod** |
| 日期 | 2026-07-15 |
| 状态 | 待实现 |
| 范围 | 准生产平台（P0 安全稳定 + P1 结构与运维） |
| 前序 | MVP PRD + E2E 本地/Dify 验证 |

---

## 1. 背景与目标

企业已有 **Dify 知识库** 与 **本地/云端 LLM**，需要 Spring AI 应用作为**客服编排中枢**，达到：

- 内网/专有云可部署上线  
- 接口有鉴权、可限流、可审计  
- 模型与知识故障可降级  
- 回答可追溯 sources  
- 运维可观测、可回滚  

### 1.1 成功标准

| # | 标准 | 验收 |
|---|------|------|
| G1 | 未携带有效凭证无法访问业务 API | 401/403 用例 |
| G2 | 会话只能被创建者/服务身份访问 | 越权用例 |
| G3 | 模型/知识调用有超时与降级 | 故障注入 |
| G4 | 限流生效 | 超阈返回 429 |
| G5 | 意图+模型+RAG+sources 可观测 | metrics + route 字段 |
| G6 | Dify 故障时有 degraded 路径或明确拒答 | 开关验证 |
| G7 | 低置信意图可转人工占位 | API 字段 |
| G8 | Docker 一键起应用+DB；冒烟脚本绿 | CI/手工 |
| G9 | 自动化测试门禁通过 | `mvn verify` |

### 1.2 非目标（本期）

- 完整工单系统 / CRM 对接  
- 企微/钉钉等多渠道中台  
- SaaS 多租户计费  
- 微服务拆分与服务网格  
- 替换 Dify 自建全量知识中台  

---

## 2. 用户与场景

| 角色 | 诉求 |
|------|------|
| 终端用户（经 BFF/网关） | 安全提问、准确答复、可引用来源 |
| 业务运营 | 查看知识状态、模型路由（只读/受控） |
| 研发/SRE | 配置模型源、限流、SLO、发布回滚 |
| 合规 | 审计查询、敏感操作留痕 |

### 主场景

1. **政策咨询（RAG）**：用户问退款 → 分类 BILLING → Dify 检索 → 强模型回答 + sources  
2. **闲聊**：CHITCHAT → 不检索 → 快模型  
3. **知识不足**：检索空 → 明确「依据不足」→ 可选转人工  
4. **上游故障**：Dify/模型超时 → 降级策略 + 错误码  
5. **滥用防护**：高频请求 → 429  

---

## 3. 功能需求

### 3.1 对话（F-CHAT）

| ID | 需求 | 优先级 |
|----|------|--------|
| F-CHAT-01 | 同步问答 `POST /api/v1/chat` | P0 |
| F-CHAT-02 | 流式问答 SSE `POST /api/v1/chat/stream` | P0 |
| F-CHAT-03 | 会话续聊（服务端校验归属） | P0 |
| F-CHAT-04 | 返回 `route`（intent/confidence/models/rag）+ `sources` | P0 |
| F-CHAT-05 | 返回 `degraded` / `handoff` 标志 | P1 |
| F-CHAT-06 | 输入长度/敏感模式校验 | P1 |

### 3.2 知识（F-KB）

| ID | 需求 | 优先级 |
|----|------|--------|
| F-KB-01 | provider=`dify` 为主路径 | P0 |
| F-KB-02 | Dify retrieve 失败可配置 fallback | P0 |
| F-KB-03 | provider=`local`/`none` 保留 | P1 |
| F-KB-04 | 检索结果带 score/title/documentId | P0 |
| F-KB-05 | 知识健康检查纳入 readiness（可选） | P1 |

### 3.3 模型网关（F-MODEL）

| ID | 需求 | 优先级 |
|----|------|--------|
| F-MODEL-01 | local/cloud 配置切换 | P0 |
| F-MODEL-02 | classifier/answer 角色分模型 | P0 |
| F-MODEL-03 | 角色级 timeout | P0 |
| F-MODEL-04 | 失败重试（幂等读）+ 熔断 | P1 |
| F-MODEL-05 | 运行时只读查询已注册模型 | P0 |

### 3.4 路由与策略（F-ROUTE）

| ID | 需求 | 优先级 |
|----|------|--------|
| F-ROUTE-01 | LLM 意图分类 | P0 |
| F-ROUTE-02 | 意图→模型映射 | P0 |
| F-ROUTE-03 | 置信度阈值 → UNKNOWN / handoff | P1 |
| F-ROUTE-04 | forceRag 配置 | P0 |

### 3.5 安全（F-SEC）

| ID | 需求 | 优先级 |
|----|------|--------|
| F-SEC-01 | API：`X-API-Key` 或 `Authorization: Bearer` | P0 |
| F-SEC-02 | Admin：独立密钥或同体系 + 角色 | P0 |
| F-SEC-03 | prod profile 禁止全开放 | P0 |
| F-SEC-04 | 限流（IP + principal） | P0 |
| F-SEC-05 | 日志脱敏（key/token） | P0 |

### 3.6 护栏与转人工（F-GUARD）

| ID | 需求 | 优先级 |
|----|------|--------|
| F-GUARD-01 | 可配置 system prompt 模板 | P1 |
| F-GUARD-02 | 无知识时强制「依据不足」策略 | P0 |
| F-GUARD-03 | handoff 占位：返回 `handoff=true` + 原因码 | P1 |
| F-GUARD-04 | 简单注入特征检测（可选拦截） | P1 |

### 3.7 观测与审计（F-OBS）

| ID | 需求 | 优先级 |
|----|------|--------|
| F-OBS-01 | requestId 全链路 | P0 |
| F-OBS-02 | metrics：请求量、延迟、意图、RAG 命中、降级、错误 | P1 |
| F-OBS-03 | route_log / audit 持久化 | P0 |
| F-OBS-04 | health / readiness | P0 |

### 3.8 交付（F-DEL）

| ID | 需求 | 优先级 |
|----|------|--------|
| F-DEL-01 | 多 profile：mock/dev/prod | P0 |
| F-DEL-02 | Dockerfile + compose（app+pg） | P0 |
| F-DEL-03 | 冒烟脚本与 Runbook | P0 |
| F-DEL-04 | 版本号 0.2.0-rc / 发布说明 | P1 |

---

## 4. 接口契约（摘要）

### 4.1 认证

```
X-API-Key: <key>
# 或
Authorization: Bearer <token>
```

### 4.2 Chat 响应扩展

```json
{
  "sessionId": "uuid",
  "answer": "...",
  "route": {
    "intent": "BILLING",
    "confidence": 0.95,
    "classifierModelId": "classifier-default",
    "answerModelId": "answer-strong",
    "ragEnabled": true
  },
  "sources": [{ "documentId": "...", "title": "...", "snippet": "...", "score": 0.9 }],
  "degraded": false,
  "degradedReasons": [],
  "handoff": false,
  "handoffReason": null
}
```

### 4.3 错误码（业务）

| 场景 | code 域 |
|------|---------|
| 未授权 | 框架/HTTP 401 |
| 限流 | HTTP 429 |
| 模型失败 | CSAI.BIZ.MODEL_* |
| 知识失败 | CSAI.BIZ.KNOWLEDGE_* |
| 会话越权 | CSAI.BIZ.SESSION_* |

---

## 5. 数据需求

- 保留：`cs_document`, `cs_chat_session`, `cs_chat_message`, `cs_route_log`  
- 新增：  
  - `cs_chat_session.owner_id`（主体标识）  
  - `cs_audit_log`（安全/降级/handoff 事件）  
  - 可选：`cs_api_client`（若采用 DB 存 key 哈希；一期可用配置文件）

---

## 6. 验收要点

见 `docs/acceptance/ACCEPTANCE-智能客服-Production-v1.md`（实现期同步完善）。

最小验收剧本：

1. 无 Key → 401  
2. 有 Key → chat 200，含 route  
3. 连续超限 → 429  
4. 停 Dify → degraded 或明确错误，不拖死进程  
5. 低置信 mock → handoff=true  
6. `docker compose up` + smoke 绿  

---

## 7. 里程碑

| 里程碑 | 内容 |
|--------|------|
| M1 | 分层骨架 + 安全/限流 |
| M2 | 韧性 + 知识/模型硬化 |
| M3 | 护栏/handoff + 观测 |
| M4 | Docker/Runbook/验收 + 0.2.0-rc |
