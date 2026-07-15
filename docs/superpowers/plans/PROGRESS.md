# 实现进度记忆

| 字段 | 值 |
|------|-----|
| 分支 | `feat/csai-mvp` |
| 当前 | **准生产 P0+P1 设计已完成，待用户确认后实施** |
| 上一里程碑 | 本地 llama + Dify E2E 冒烟通过 |
| 目标版本 | **0.2.0-rc.1** |

## 用户确认范围

**P0+P1 准生产平台**（演进式硬化，非推倒微服务）：

- P0：鉴权、限流、超时熔断、审计、观测基础、Docker 交付、Dify 稳定路径  
- P1：洁净分层、护栏、转人工占位、Admin 加固、Testcontainers/SLO  

## 文档入口

| 文档 | 路径 |
|------|------|
| 架构评估 | `docs/analysis/ARCHITECTURE-生产级评估-v1.md` |
| 生产 PRD | `docs/requirements/PRD-智能客服-Production-v1.md` |
| 设计规格 | `docs/superpowers/specs/2026-07-15-csai-production-refactor-design.md` |
| 实现计划 | `docs/superpowers/plans/2026-07-15-csai-production-p0p1-plan.md` |
| 验收清单 | `docs/acceptance/ACCEPTANCE-智能客服-Production-v1.md` |
| Dify + 模型 | `docs/development/DIFY-知识库与可插拔模型.md` |

## 阻塞

⏳ **等待用户确认设计规格** → 确认后按 Phase 1→7 实施代码

## 已完成（MVP）

- [x] Framework 接入、Chat/RAG/Router/Admin  
- [x] Dify 知识主路径 + local/cloud 模型切换  
- [x] E2E：local-qwen + Dify（含 retrieve fallback）  
