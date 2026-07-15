# 验收清单 — 智能客服 Production v1（P0+P1）

| 属性 | 值 |
|------|-----|
| 版本 | 1.0 |
| 对应 PRD | `PRD-智能客服-Production-v1.md` |
| 状态 | 代码已实现；生产环境需现场勾选 |

## G1 鉴权

- [x] 无 `X-API-Key` 访问 `/api/v1/chat` → 401（单测 ApiKeyAuthFilterTest）  
- [x] 错误 Key → 401  
- [ ] 正确 CLIENT key → 200（联调/冒烟）  

## G2 会话归属

- [x] owner_id 写入与跨 principal 拒绝（代码路径）  
- [ ] A/B 双 Key 现场验证  

## G3 超时与降级

- [x] 模型超时 → MODEL_TIMEOUT  
- [x] Dify 失败 → degraded + fallback  

## G4 限流

- [x] RateLimitFilter 超限 429（代码）  
- [ ] 现场刷量验证  

## G5 可观测

- [x] 响应含 route/sources/degraded/handoff  
- [x] metrics `csai.*`  
- [x] requestId（框架）  

## G6 知识路径

- [x] provider=dify + degraded 原因  
- [x] provider=none  

## G7 转人工占位

- [x] 低置信 handoff（单测）  
- [x] audit HANDOFF  

## G8 交付

- [x] Dockerfile + compose profile full  
- [x] scripts/smoke-prod.sh  
- [x] RUNBOOK  

## G9 测试门禁

- [x] `mvn test` 绿（Docker 集成测 skip）  

## 回归（MVP 能力）

- [x] local/cloud model-source  
- [x] mock profile  
- [ ] Admin + ADMIN key 浏览器联调  
