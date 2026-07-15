# 产品需求文档（PRD）— 智能客服系统 MVP

| 属性 | 值 |
|------|-----|
| 版本 | **1.1** |
| 日期 | 2026-07-15 |
| 产品名称 | 企业智能客服（Customer Service AI） |
| 阶段 | MVP |
| 技术底座 | [microservice-framework](https://github.com/andy-library/microservice-framework) 1.0.0-alpha.1 |

### 修订记录

| 版本 | 说明 |
|------|------|
| 1.0 | 初版 MVP 需求 |
| 1.1 | 增加企业技术底座强制依赖与相关验收 |

---

## 1. 问题陈述

企业客服存在：

- 多模型能力分散，无法按问题类型自动选择模型。  
- 业务知识无法稳定注入回答，易幻觉。  
- 若脱离统一微服务基座，将重复建设 Web/安全/日志/观测/数据访问能力，难以与企业中台对齐。

## 2. 目标用户

| 角色 | 诉求 |
|------|------|
| 客服/业务运营 | 有依据的答复与来源追溯 |
| 知识管理员 | 上传与维护企业文档 |
| 平台研发 | 基于 microservice-framework 扩展 AI 能力，可观测、可配置 |

## 3. 业务目标

1. 提问 → 自动路由模型 →（按需）RAG → 可引用回答。  
2. 工程上 **100% 建立在 microservice-framework 基座** 之上。  
3. 为后续拆服务、接配置中心/网关预留边界。

## 4. 范围

### 4.1 In Scope（MVP）

| ID | 需求 | 优先级 |
|----|------|--------|
| R0 | 继承 framework starter-parent，启用 common/json/web/logging/observability/database/async/security | P0 |
| R1 | ≥2 个 OpenAI-compatible 模型配置化接入 | P0 |
| R2 | LLM 意图分类路由 | P0 |
| R3 | 文档上传（PDF/MD/TXT）向量化 | P0 |
| R4 | RAG 回答附带 sources | P0 |
| R5 | 同步问答 API（**ApiResponse** 包装） | P0 |
| R6 | SSE 流式问答 | P1 |
| R7 | 简易管理后台 | P0 |
| R8 | 会话多轮上下文 | P1 |
| R9 | 路由日志（含 requestId） | P1 |
| R10 | Compose + 框架安装说明 + 验收文档 | P0 |

### 4.2 Out of Scope

- 微服务拆分、Nacos/Apollo 生产配置中心  
- 完整 SSO/RBAC  
- 工单/转人工/渠道接入  
- 多租户、计费、内容安全平台  
- 框架内新增官方 AI Starter（可作为二期回馈）  

## 5. 用户故事

### US-0 基座合规（新增）

**作为** 平台研发，**我希望** 应用基于 microservice-framework 构建，**以便** 复用企业统一能力并满足工程治理。

**验收：**

- parent 为 `microservice-framework-starter-parent:1.0.0-alpha.1`。  
- 响应体为框架 `ApiResponse`（`code/message/data/requestId`）。  
- 存在 RequestId 传递；health 可用。  

### US-1 智能问答

**作为** 业务用户，**我希望** 自然语言提问得到答案。  

**验收：** `POST /api/v1/chat` 的 `data.answer` 非空，且含 `data.route`。

### US-2 知识增强

**作为** 业务用户，**我希望** 答案基于企业知识库。  

**验收：** 已入库文档相关问题 `data.sources` 非空；无关问题不捏造内部条款。

### US-3 知识管理

**作为** 知识管理员，**我希望** 上传/列表/删除文档。  

**验收：** 状态 INDEXED；删除后不再命中。

### US-4 路由可观测

**作为** 平台研发，**我希望** 看到意图与模型选择。  

**验收：** `data.route` 含 intent、模型 id；可查 models/intents API。

### US-5 对话测试台

**作为** 运营/研发，**我希望** 在网页测试问答。  

**验收：** `/admin/chat` 展示 answer、route、sources。

## 6. 功能需求明细

### 6.0 技术底座（强制）

- 构建依赖框架 Parent/BOM；业务 Starter **不写版本号**。  
- Spring AI 由业务 `dependencyManagement` 引入 1.1.8 BOM。  
- 配置：`framework.*` 与 `csai.*` 分离。  
- 数据：PostgreSQL + Flyway；禁止生产自动乱建表。  
- 安全：MVP permit 演示路径；文档标明生产收紧。

### 6.1～6.5

与 v1.0 一致：模型网关、智能路由、知识库 RAG、对话、管理后台（细节见设计规格 v1.1）。

## 7. 非功能需求

| 类型 | 要求 |
|------|------|
| 运行时 | OpenJDK 21 |
| 基座 | microservice-framework 1.0.0-alpha.1 |
| 框架 | Boot 3.3.13 / Cloud 2023.0.6（不可擅自覆盖） |
| AI | Spring AI 1.1.8 |
| 安全 | 密钥环境变量；上传限制；日志脱敏 |
| 可维护 | 业务包与框架包边界清晰 |

## 8. 依赖与假设

- 可 clone 并 `mvn install` 框架（或内网仓库已发布 alpha）。  
- 可提供 ≥2 个 OpenAI-compatible 端点，或使用 mock profile。  
- Embedding 维度与向量列一致。  

## 9. 里程碑

| 里程碑 | 产出 |
|--------|------|
| M0 底座接入 | PR-0：框架 parent + 基础 starters 可运行 |
| M1 设计冻结 | PRD/设计 v1.1 确认 |
| M2 主链路 | 网关 + 路由 + RAG + Chat |
| M3 验收 | 管理台 + 测试 + 验收清单 |

## 10. 验收总则

全部 **P0**（含 US-0）通过，且验收清单勾选完成，视为 MVP 通过。

---

*关联设计：`docs/superpowers/specs/2026-07-15-intelligent-customer-service-design.md`（v1.1）*  
*底座分析：`docs/analysis/microservice-framework-foundation-analysis.md`*
