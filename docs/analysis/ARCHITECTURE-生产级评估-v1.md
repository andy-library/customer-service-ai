# 智能客服系统 — 生产级架构评估 v1

| 属性 | 值 |
|------|-----|
| 日期 | 2026-07-15 |
| 对象 | customer-service-ai（feat/csai-mvp） |
| 视角 | 资深架构师 / 可上产线标准 |
| 结论 | **MVP 可演示；P0+P1 硬化后方可准生产** |
| 目标档位 | **P0+P1 准生产平台**（用户确认） |

---

## 1. 系统现状摘要

当前系统是 **模块化单体**：

- 技术底座：`microservice-framework` 1.0.0-alpha.1 + Spring Boot 3.3.13 + Spring AI 1.1.8  
- 编排：意图分类 → 模型路由 →（可选）知识检索 → 回答生成  
- 知识：默认 **Dify Dataset**；可选 local PgVector  
- 模型：`csai.model-source=local|cloud` 可插拔  
- 已验证：本机 llama.cpp（:18080/:18081）+ Dify K8s + 端到端聊天  

**定位：** 验证了「编排层归属 Spring AI、知识归属 Dify」的正确分工；工程能力仍停留在演示态。

---

## 2. 缺陷矩阵

### 2.1 P0（安全与稳定性 — 阻断上线）

| ID | 缺陷 | 根因 | 上线后果 |
|----|------|------|----------|
| S-01 | API/Admin 全开放 | security permit-paths 过宽 | 数据泄露、算力盗用 |
| S-02 | 无会话归属 | sessionId 客户端自带无校验 | 越权读写历史 |
| S-03 | 模型调用无硬超时 | timeout 配置未落到 HTTP 客户端 | 线程耗尽 |
| S-04 | 无限流 | 无 gateway/app 限流 | 账单/GPU 打满 |
| S-05 | 密钥管理薄弱 | 仅 `.env` | 泄露与环境漂移 |
| S-06 | Dify retrieve 不可靠 | 上游 1.16-rc1 bug | RAG 空结果或 500 |

### 2.2 P1（质量、结构、运维 — 准生产必补）

| ID | 缺陷 | 根因 | 影响 |
|----|------|------|------|
| Q-01 | 分层不足 | 编排/仓储/DTO 同包 | 难测、难演进 |
| Q-02 | 无事务边界 | 多表写入无 `@Transactional` | 半成功状态 |
| Q-03 | 线程池无治理 | `newCachedThreadPool` | OOM/线程风暴 |
| Q-04 | 可观测不全 | 仅日志 latency | 无法 SLO |
| Q-05 | 护栏弱 | 固定 prompt | 幻觉/注入 |
| Q-06 | 路由无阈值 | 低置信仍强答 | 错误回答 |
| Q-07 | Admin 未加固 | 与业务 API 同权限 | 运营风险 |
| Q-08 | 测试不足 | 集成常 skip | 回归无门禁 |
| Q-09 | 交付不全 | 无 app 镜像/Runbook | 无法标准发布 |

### 2.3 P2（产品能力 — 二期）

工单、多渠道、质检中台、多租户 SaaS、完整 RBAC、微服务拆分。

---

## 3. 优化原则

1. **编排在 Spring，知识在 Dify，模型可插拔** — 已验证，保留。  
2. **端口/适配器** — 领域不依赖 Dify/OpenAI 细节。  
3. **失败可预期** — 每个外部依赖：超时、降级、指标、审计。  
4. **默认安全** — 未显式授权即拒绝。  
5. **可回滚交付** — 镜像 + 配置 + 迁移 + 冒烟脚本。  
6. **兼容演进** — 尽量保持 `/api/v1` 形状，加安全与字段扩展。

---

## 4. 目标架构（P0+P1）

见 `docs/superpowers/specs/2026-07-15-csai-production-refactor-design.md`。

核心变化：

```
interface (REST/SSE/Admin)
    → application (ChatUseCase, KnowledgeUseCase, AdminUseCase)
        → domain (Intent, Session, Policy, Guardrail)
        → ports (ModelPort, KnowledgePort, SessionRepository, AuditPort)
            → adapters (OpenAI, Dify, JDBC, Micrometer)
```

---

## 5. 与当前代码映射

| 现有 | 目标 |
|------|------|
| `ChatOrchestrator` | `application.chat.ChatUseCase` + 策略协作 |
| `ModelGateway*` | `port.ModelPort` + `adapter.openai` + Resilience |
| `KnowledgeRetriever*` | `port.KnowledgePort` + Dify/Local/Composite |
| `RoutingService` | `domain.routing` + 置信度阈值 |
| `AdminController` | 独立 security chain + 只读运维视图 |
| `CsaiProperties` | 分模块 `@ConfigurationProperties` + 启动校验 |
| 无 | `SecurityConfig`, `RateLimitFilter`, `ResilienceConfig`, `Metrics`, `Dockerfile` |

---

## 6. 非功能目标（SLO 草案）

| 指标 | 目标（内网准生产） |
|------|-------------------|
| 可用性 | 99.5%（依赖模型/Dify 时按依赖降级） |
| 同步问答 P95 | 本地模型 ≤ 60s；云端 ≤ 15s（可配） |
| 错误率（5xx） | < 1%（业务拒答不算 5xx） |
| 认证失败 | 全量拒绝未授权 |
| 审计 | 100% 写 route/audit 日志 |

---

## 7. 风险与依赖

| 风险 | 缓解 |
|------|------|
| Dify RC 不稳定 | Composite Knowledge：retrieve → fallback → 明确 degraded 标记 |
| 本地大模型慢 | 分类/回答分超时；流式优先；线程池隔离 |
| Framework alpha | 不改 parent；业务侧补齐缺口 |
| 安全引入破坏联调 | `dev` profile 可放宽；`prod` 强制鉴权 |

---

## 8. 建议实施顺序

见实现计划 `docs/superpowers/plans/2026-07-15-csai-production-p0p1-plan.md`：

1. 文档与基线锁定  
2. 分层骨架 + 端口  
3. 安全与限流  
4. 韧性与线程池  
5. 知识/模型硬化  
6. 护栏与转人工占位  
7. 观测与审计  
8. 交付与验收  
