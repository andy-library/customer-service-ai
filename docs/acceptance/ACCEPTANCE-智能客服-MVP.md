# 验收清单 — 智能客服系统 MVP

| 属性 | 值 |
|------|-----|
| 版本 | **1.1** |
| 日期 | 2026-07-15 |
| 技术底座 | microservice-framework 1.0.0-alpha.1 |
| 验收人 | ________________ |
| 日期签字 | ________________ |

说明：全部 **P0** 通过即为 MVP 通过。P1 未完成记遗留项。

---

## A. 环境、底座与交付物（P0）

| # | 项 | 通过 | 备注 |
|---|----|:----:|------|
| A1 | 仓库含源码、`pom.xml`、`docker-compose.yml`、`.env.example`、框架安装说明/脚本 | ☐ | |
| A2 | 存在 PRD、设计 v1.1、开发文档、底座分析、本验收清单 | ☐ | |
| A3 | README 含：安装 framework → 启动 DB → 启动应用 | ☐ | |
| A4 | `pom.xml` parent 为 `microservice-framework-starter-parent:1.0.0-alpha.1` | ☐ | |
| A5 | OpenJDK 21；Boot **3.3.13**（未覆盖保护版本）；Spring AI 1.1.x | ☐ | |
| A6 | 依赖含 common/json/web/logging/observability/database/security（及文档声明的 async） | ☐ | |
| A7 | `docker compose up -d` 可启动 PostgreSQL(pgvector) | ☐ | |
| A8 | 应用可启动；`/actuator/health` 为 UP | ☐ | |

## B. 框架契约（P0）

| # | 项 | 通过 | 备注 |
|---|----|:----:|------|
| B1 | 业务 JSON 接口外层为 ApiResponse：`code/message/data` | ☐ | |
| B2 | 成功时 `code` 为成功码（默认 0）；`data` 承载业务字段 | ☐ | |
| B3 | 响应含 `requestId`（或文档说明关闭条件） | ☐ | |
| B4 | 非法参数返回框架风格错误，而非未处理 500 堆栈页 | ☐ | |
| B5 | MVP 下 `/api/v1/**` 与 `/admin/**` 可按设计匿名访问 | ☐ | |

## C. 模型网关（P0）

| # | 项 | 通过 | 备注 |
|---|----|:----:|------|
| C1 | 配置 ≥2 个 OpenAI-compatible 相关模型 | ☐ | |
| C2 | `GET /api/v1/models` 的 data 列表不含 apiKey | ☐ | |
| C3 | 错误 key 时有明确错误与日志，进程不崩溃 | ☐ | |

## D. 智能路由（P0）

| # | 项 | 通过 | 备注 |
|---|----|:----:|------|
| D1 | 产品类问题 intent 合理且 answerModelId 符合映射 | ☐ | |
| D2 | 闲聊倾向 CHITCHAT 并路由到 fast 模型（按配置） | ☐ | |
| D3 | 分类失败降级 UNKNOWN + 默认模型 | ☐ | |
| D4 | `data.route` 含 intent、confidence、模型 id、ragEnabled | ☐ | |

## E. 知识库与 RAG（P0）

| # | 项 | 通过 | 备注 |
|---|----|:----:|------|
| E1 | 可上传 md/txt（及可用的文本 PDF） | ☐ | |
| E2 | 上传后状态 INDEXED | ☐ | |
| E3 | 相关提问 sources 非空 | ☐ | |
| E4 | 答案与知识相关 | ☐ | |
| E5 | 删除后不再命中 | ☐ | |
| E6 | 无关问题声明依据不足 / 不捏造内部条款 | ☐ | |

## F. 对话 API（P0/P1）

| # | 项 | 优先级 | 通过 | 备注 |
|---|----|--------|:----:|------|
| F1 | `POST /api/v1/chat` 返回 data.answer | P0 | ☐ | |
| F2 | sessionId 多轮上下文 | P1 | ☐ | |
| F3 | SSE 流式可用且不被错误 JSON 包装破坏 | P1 | ☐ | |
| F4 | 空 message 业务/校验错误 | P0 | ☐ | |

## G. 管理后台（P0）

| # | 项 | 通过 | 备注 |
|---|----|:----:|------|
| G1 | `/admin` 可访问 | ☐ | |
| G2 | 知识库上传/列表/删除 | ☐ | |
| G3 | 对话测试台展示 answer、route、sources | ☐ | |
| G4 | 模型与路由只读页 | ☐ | |

## H. 测试与质量（P0）

| # | 项 | 通过 | 备注 |
|---|----|:----:|------|
| H1 | `./mvnw test`（mock 或 testcontainers）通过 | ☐ | |
| H2 | 路由单元测试存在 | ☐ | |
| H3 | RAG 或编排测试存在 | ☐ | |
| H4 | 至少 1 个 ApiResponse/契约向测试 | ☐ | |

## I. 安全基线（P0）

| # | 项 | 通过 | 备注 |
|---|----|:----:|------|
| I1 | 仓库无真实 apiKey | ☐ | |
| I2 | 拒绝非法扩展名或超大文件 | ☐ | |
| I3 | 文档标明生产须收紧 security permit | ☐ | |

---

## 验收结论

- [ ] **通过** — 全部 P0 通过  
- [ ] **有条件通过** — 遗留：________________  
- [ ] **不通过** — 阻塞项：________________  

验收人签名：________________  日期：________________
