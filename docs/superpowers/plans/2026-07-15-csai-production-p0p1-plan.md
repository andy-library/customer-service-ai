# 实现计划 — CSAI 准生产 P0+P1

| 属性 | 值 |
|------|-----|
| 分支建议 | `feat/csai-prod-p0p1`（或继续 `feat/csai-mvp`） |
| 版本 | 0.2.0-rc.1 |
| 设计 | `docs/superpowers/specs/2026-07-15-csai-production-refactor-design.md` |

---

## 原则

- 小步可运行：每完成一阶段 `mvn test` 绿  
- 兼容 `/api/v1`：先加字段与安全，再挪包  
- 不引入 Boot 升级  
- 密钥不入库  

---

## Phase 0 — 文档与基线（已完成/进行中）

- [x] 架构评估  
- [x] PRD Production  
- [x] 设计规格  
- [ ] 用户确认设计  
- [ ] 锁定分支与版本号  

---

## Phase 1 — 分层骨架（不改行为）

**目标：** 新包结构 + 端口接口 + 旧实现适配委托  

1. 创建 `domain.port`：`ModelPort`, `KnowledgePort`, `SessionPort`, `AuditPort`  
2. 创建 `application.chat.ChatUseCase` 包装现有 `ChatOrchestrator`（或逐步搬迁）  
3. Controller 改调 UseCase  
4. 测试保持绿  

**完成标准：** 行为与重构前一致，包结构可见  

---

## Phase 2 — 安全与限流（P0）

1. `SecurityProperties` + API Key 过滤器  
2. `prod` 强制安全；`application-prod.yml`  
3. Session `owner_id` migration + 校验  
4. Rate limit filter  
5. Admin 角色限制  
6. 测试：401/403/429/越权  

**完成标准：** G1 G2 G4  

---

## Phase 3 — 韧性与线程池（P0/P1）

1. 引入 resilience4j（或 Spring 重试 + 自研 TimeLimiter）  
2. ModelPort 超时  
3. Dify RestClient 超时  
4. 替换 CachedThreadPool → 有界池  
5. 故障注入单测  

**完成标准：** G3  

---

## Phase 4 — 知识/模型硬化（P0/P1）

1. KnowledgePort 统一 degraded 原因  
2. Dify fallback 指标与审计  
3. Model profile 启动校验  
4. Runtime API 含 security 后只读  

**完成标准：** G5 G6  

---

## Phase 5 — 护栏与转人工（P1）

1. Prompt 外部化  
2. requireEvidence  
3. HandoffPolicy + 响应字段  
4. 审计 HANDOFF  

**完成标准：** G7  

---

## Phase 6 — 观测（P1）

1. Micrometer counters/timers  
2. readiness contributor  
3. route_log 扩展字段  

**完成标准：** G5 增强  

---

## Phase 7 — 交付（P0/P1）

1. Dockerfile  
2. compose app 服务  
3. RUNBOOK  
4. smoke-prod.sh  
5. ACCEPTANCE 勾选  
6. 版本 0.2.0-rc.1 发布说明  

**完成标准：** G8 G9  

---

## 任务依赖图

```
P0 docs → P1 skeleton → P2 security → P3 resilience
                              ↓              ↓
                         P4 knowledge/model ←┘
                              ↓
                         P5 guard/handoff
                              ↓
                         P6 metrics
                              ↓
                         P7 delivery
```

---

## 回滚策略

- 配置：`csai.security.enabled=false` 仅限紧急（prod 禁止）  
- 镜像：保留上一 rc tag  
- DB：migration 仅 ADD 可空列/新表，可前向兼容  
