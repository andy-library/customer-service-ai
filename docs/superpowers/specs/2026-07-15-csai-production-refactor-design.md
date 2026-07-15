# 设计规格 — 智能客服准生产重构（P0+P1）

| 属性 | 值 |
|------|-----|
| 版本 | **1.0** |
| 日期 | 2026-07-15 |
| 状态 | **待用户确认后实现** |
| 范围 | P0 安全稳定 + P1 分层/护栏/观测/交付 |
| 策略 | **演进式硬化（方案 A）**，API 尽量兼容 `/api/v1` |
| 关联 PRD | `docs/requirements/PRD-智能客服-Production-v1.md` |
| 评估 | `docs/analysis/ARCHITECTURE-生产级评估-v1.md` |

---

## 1. 设计目标

在不推翻 Dify + 可插拔模型验证成果的前提下，将应用提升为：

1. **默拒安全**的内网准生产服务  
2. **依赖故障可降级**的编排中枢  
3. **可测、可观测、可发布**的工程形态  

---

## 2. 逻辑架构

```
┌─────────────────────────────────────────────────────────────────┐
│ interface                                                        │
│  rest (Chat/Knowledge/Model/Runtime) · sse · admin(web)          │
│  security filters · rate limit · request validation              │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│ application                                                      │
│  ChatUseCase · KnowledgeQueryUseCase · RuntimeQueryUseCase       │
│  编排事务边界、DTO 组装、策略调用                                  │
└──────────────┬─────────────────────────────┬────────────────────┘
               │                             │
┌──────────────▼──────────────┐ ┌────────────▼────────────────────┐
│ domain                       │ │ domain.policy                   │
│  Intent · Session · Message  │ │  RoutingPolicy · GuardrailPolicy│
│  KnowledgeHit · RouteDecision│ │  HandoffPolicy · ModelPolicy    │
└──────────────┬──────────────┘ └────────────┬────────────────────┘
               │ ports                       │
┌──────────────▼─────────────────────────────▼────────────────────┐
│ infrastructure / adapters                                        │
│  openai ModelAdapter · dify KnowledgeAdapter · local Vector      │
│  jdbc SessionRepo · audit · micrometer · resilience4j            │
└──────────────────────────────────────────────────────────────────┘
```

### 2.1 包结构（目标）

```
com.enterprise.csai
  CustomerServiceAiApplication
  interface
    rest
    admin
    security
  application
    chat
    knowledge
    runtime
  domain
    model
    policy
    port          # interfaces only
  infrastructure
    config
    persistence
    model
    knowledge
    observability
    resilience
```

迁移策略：**先建新包并委托旧类 → 再搬迁实现 → 删除死代码**，保证可运行窗口。

---

## 3. 核心用例

### 3.1 ChatUseCase

```
input: principal, ChatCommand
1. validate input (length, blank)
2. loadOrCreateSession(owner=principal.id)
3. route = RoutingService.classify(message)  // timeout + degrade UNKNOWN
4. apply HandoffPolicy(confidence) → maybe handoff short-circuit
5. if rag needed: hits = KnowledgePort.search (circuit + fallback)
6. messages = PromptFactory.build(system, history, hits, user)
7. answer = ModelPort.chat(answerModelId) // timeout
8. persist session/messages/route/audit in one transaction
9. emit metrics
10. return ChatResult(answer, route, sources, degraded, handoff)
```

### 3.2 KnowledgePort

```java
public interface KnowledgePort {
  String provider();
  List<KnowledgeHit> search(String query, int topK);
  Health health();
}
```

实现：

- `DifyKnowledgeAdapter`：retrieve 优先；失败/空 → `SegmentFallback`（标记 `degraded=RETRIEVE_FALLBACK`）  
- `LocalVectorKnowledgeAdapter`：`provider=local`  
- `NoOpKnowledgeAdapter`：`provider=none`  
- `CompositeKnowledgeAdapter`（可选）：primary + secondary  

### 3.3 ModelPort

```java
public interface ModelPort {
  String chat(String modelId, List<Message> messages, Duration timeout);
  Flux<String> stream(String modelId, List<Message> messages, Duration timeout);
  List<ModelDescriptor> list();
}
```

- Registry 启动时装载 local 或 cloud profile  
- Resilience：TimeLimiter + CircuitBreaker + bulkhead（按 classifier/answer 分组）  
- `timeoutMs` 必须传到底层 HTTP  

---

## 4. 安全设计

### 4.1 认证

| Profile | 行为 |
|---------|------|
| `mock` | 测试用固定 principal 或 test key |
| `dev` | 可配 `csai.security.enabled=false`（默认 true 推荐） |
| `prod` | **强制** `security.enabled=true`，启动失败若无 keys |

**一期采用 API Key：**

- 配置：`csai.security.api-keys[0].id/key/roles`  
- Header：`X-API-Key`  
- Principal：`ApiKeyPrincipal(id, roles)`  
- Admin 要求 `ROLE_ADMIN`；Chat 要求 `ROLE_CLIENT` 或 `ROLE_ADMIN`

### 4.2 会话归属

- `cs_chat_session.owner_id` = principal.id  
- 访问他人 session → `SESSION_FORBIDDEN`  

### 4.3 限流

- Bucket4j 或简单 `RateLimiter`（Guava/Resilience4j）  
- 键：`principalId` + 路径组  
- 默认：60 req/min/client（可配）  
- 超限：HTTP 429 + `Retry-After`  

### 4.4 Admin

- `/admin/**` 需 `ROLE_ADMIN`  
- 生产建议关闭或仅内网 Ingress  

---

## 5. 韧性设计

| 依赖 | 超时（默认） | 重试 | 熔断 | 降级 |
|------|--------------|------|------|------|
| Classifier | 15s | 0 | 打开后 30s | UNKNOWN + handoff 可选 |
| Answer | 60s local / 30s cloud | 0 | 打开后失败快速 | 业务错误码 |
| Dify retrieve | 5s | 1 | 打开后走 fallback | segment fallback / empty |
| Embedding(local) | 10s | 0 | — | 仅 local provider |

线程池：

- `csai-chat-stream`：有界队列 + CallerRuns 或拒绝策略  
- 禁止无界 CachedThreadPool  

---

## 6. 护栏与转人工

### 6.1 GuardrailPolicy

- System prompt 外部化：`classpath:prompts/system-cs.md` + 配置覆盖  
- `requireEvidence`：rag 开启且 hits 空 → 回答模板强制「依据不足」  
- 可选：检测 “ignore previous instructions” 等模式 → 拒答  

### 6.2 HandoffPolicy

触发条件（可配）：

- `confidence < threshold`（默认 0.55）  
- intent == UNKNOWN 且 `handoffOnUnknown=true`  
- 用户话术命中「转人工」  

行为（一期占位）：

- 仍可返回简短安抚话术 **或** 直接返回 handoff 消息  
- 响应字段 `handoff=true`, `handoffReason=LOW_CONFIDENCE|USER_REQUEST|UNKNOWN_INTENT`  
- 写 audit 事件 `HANDOFF`  

---

## 7. 数据模型变更

### V2__production_harden.sql

```sql
ALTER TABLE cs_chat_session ADD COLUMN IF NOT EXISTS owner_id VARCHAR(128);
CREATE INDEX IF NOT EXISTS idx_cs_session_owner ON cs_chat_session(owner_id);

CREATE TABLE IF NOT EXISTS cs_audit_log (
  id UUID PRIMARY KEY,
  principal_id VARCHAR(128),
  event_type VARCHAR(64) NOT NULL,
  session_id UUID,
  detail JSONB,
  request_id VARCHAR(128),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_cs_audit_created ON cs_audit_log(created_at);
```

`cs_route_log` 扩展（可选同一 migration）：

- `degraded BOOLEAN`  
- `handoff BOOLEAN`  
- `owner_id VARCHAR(128)`  

---

## 8. 配置模型

```yaml
csai:
  security:
    enabled: true
    api-keys:
      - id: demo-client
        key: ${CSAI_API_KEY_DEMO}
        roles: [CLIENT]
      - id: demo-admin
        key: ${CSAI_API_KEY_ADMIN}
        roles: [ADMIN, CLIENT]
    rate-limit:
      enabled: true
      requests-per-minute: 60
  resilience:
    classifier-timeout: 15s
    answer-timeout: 60s
    dify-timeout: 5s
  routing:
    confidence-threshold: 0.55
    handoff-on-unknown: true
  guardrail:
    require-evidence: true
  knowledge:
    provider: dify
    dify:
      segment-fallback: true
      mark-degraded-on-fallback: true
  model-source: local
  # ... existing models/cloud ...
```

**启动校验：** `prod` 下 security.enabled、至少一个 api-key、DB URL 非空。

---

## 9. 可观测

### Metrics（Micrometer）

- `csai.chat.requests` (tag: intent, status, degraded, handoff)  
- `csai.chat.latency` (timer)  
- `csai.model.invoke` (tag: modelId, role, outcome)  
- `csai.knowledge.search` (tag: provider, outcome, fallback)  

### Health

- `liveness`：进程  
- `readiness`：DB up；可选 Dify ping / 模型 registry non-empty  

### 日志

- 结构化字段：requestId, principalId, sessionId, intent, modelId, latencyMs, degraded  

---

## 10. 交付

| 工件 | 说明 |
|------|------|
| `Dockerfile` | JRE 21 多阶段构建 |
| `docker-compose.yml` | postgres + app |
| `docs/operations/RUNBOOK.md` | 启停、配置、故障 |
| `scripts/smoke-prod.sh` | 鉴权+chat+限流冒烟 |
| 版本 | `0.2.0-rc.1` |

---

## 11. 测试策略

| 层 | 内容 |
|----|------|
| 单元 | Policy、Parser、Dify parse、ProfileResolver |
| 切片 | Security filter、Rate limit |
| 应用 | ChatUseCase mock ports |
| 集成 | Testcontainers PG + mock 模型 profile |
| 冒烟 | 真实/半真实脚本 |

---

## 12. 风险与决策记录

| 决策 | 选择 | 原因 |
|------|------|------|
| 重构方式 | 演进式分层 | 保护 Dify/本地已验证路径 |
| 鉴权 | API Key 一期 | 实现快；可换 JWT |
| Dify bug | fallback + degraded 标记 | 不阻塞上线 |
| 转人工 | 占位字段 | 不引入工单系统 |
| 框架 | 不升级 Boot | parent 锁定 |

---

## 13. 实施分期

见 `docs/superpowers/plans/2026-07-15-csai-production-p0p1-plan.md`。

---

## 14. 验收映射

PRD 成功标准 G1–G9 均需在 `docs/acceptance/ACCEPTANCE-智能客服-Production-v1.md` 勾选。
