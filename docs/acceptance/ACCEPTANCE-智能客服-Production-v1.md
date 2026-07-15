# 验收清单 — 智能客服 Production v1（P0+P1）

| 属性 | 值 |
|------|-----|
| 版本 | 1.0 |
| 对应 PRD | `PRD-智能客服-Production-v1.md` |
| 状态 | 待实现后勾选 |

## G1 鉴权

- [ ] 无 `X-API-Key` 访问 `/api/v1/chat` → 401  
- [ ] 错误 Key → 401  
- [ ] 正确 CLIENT key → 200  

## G2 会话归属

- [ ] A 创建 session，B 用自己的 key 访问 A 的 sessionId → 403  
- [ ] A 续聊自己的 session → 200  

## G3 超时与降级

- [ ] 模拟模型超时 → 业务错误，不挂死  
- [ ] 模拟 Dify 失败 → fallback 或 degraded，进程存活  

## G4 限流

- [ ] 短时间超 RPM → 429  

## G5 可观测

- [ ] 响应含 route + sources（RAG 场景）  
- [ ] metrics 端点可见 `csai.*`（或日志等价）  
- [ ] requestId 存在  

## G6 知识路径

- [ ] provider=dify 检索成功或 degraded 明确  
- [ ] provider=none 不检索  

## G7 转人工占位

- [ ] 低置信/UNKNOWN 配置下 `handoff=true`  
- [ ] audit 有 HANDOFF 事件（或 route_log 标记）  

## G8 交付

- [ ] `docker compose up` 可起  
- [ ] `scripts/smoke-prod.sh` 通过  
- [ ] RUNBOOK 可按文档启停  

## G9 测试门禁

- [ ] `mvn test` / `mvn verify` 绿  

## 回归（MVP 能力）

- [ ] local/cloud model-source 切换  
- [ ] Admin 在 ADMIN key 下可访问  
- [ ] mock profile 离线可测  
