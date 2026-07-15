# 实现进度记忆

| 字段 | 值 |
|------|-----|
| 分支 | `feat/csai-mvp` |
| 版本 | **0.2.0-rc.1** |
| 当前 | **准生产 P0+P1 代码已落地，`mvn test` 全绿** |
| 设计 | 用户确认 P0+P1 准生产平台 |

## 已交付（0.2.0-rc.1）

### P0
- [x] API Key 鉴权（`CSAI_SECURITY_ENABLED`）+ Admin 角色
- [x] 限流（滑动窗口 RPM）→ 429
- [x] 会话 `owner_id` 归属校验
- [x] 模型/Dify 超时
- [x] 有界 stream 线程池
- [x] 审计表 `cs_audit_log` + route_log 扩展
- [x] Dockerfile / compose profile full / RUNBOOK / smoke-prod.sh

### P1
- [x] 端口：`KnowledgePort` / `ModelPort`
- [x] 策略：`GuardrailPolicy` / `HandoffPolicy`
- [x] 响应：`degraded` / `handoff` 字段
- [x] Micrometer：`csai.chat.*` / `csai.knowledge.*` / `csai.model.*`
- [x] Health indicator `csai`
- [x] 单测：编排/鉴权/handoff 等 30+  

## 上线开关

```bash
export CSAI_SECURITY_ENABLED=true
export CSAI_API_KEY_CLIENT=...
export CSAI_API_KEY_ADMIN=...
# 可选 prod profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod \
  -Dspring-boot.run.arguments=--server.port=8081
```

## 文档

| 文档 | 路径 |
|------|------|
| 架构评估 | `docs/analysis/ARCHITECTURE-生产级评估-v1.md` |
| PRD | `docs/requirements/PRD-智能客服-Production-v1.md` |
| 设计 | `docs/superpowers/specs/2026-07-15-csai-production-refactor-design.md` |
| Runbook | `docs/operations/RUNBOOK.md` |
| 验收 | `docs/acceptance/ACCEPTANCE-智能客服-Production-v1.md` |

## 已知限制

- Dify 1.16-rc1 retrieve 仍可能 500 → segment-fallback + degraded
- Admin 浏览器未内置 API Key 注入（需代理/Header 工具或 security=false 内网）
- Testcontainers 集成测在无 Docker 环境 skip
