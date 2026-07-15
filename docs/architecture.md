# 系统架构

作者：**andy yang**

## 定位

**customer-service-ai** 是模块化单体应用，负责企业智能客服对话编排：

1. 调用方鉴权与限流  
2. LLM 意图分类  
3. 按策略选择回答模型  
4. 检索企业知识（默认 Dify）  
5. 生成可引用 sources 的回答  
6. 持久化会话、路由日志与审计  
7. 暴露指标与健康检查  

本项目**不是**完整工单系统或多渠道中台，而是 **AI 编排层**。

## 逻辑架构

```
                 ┌──────────────────────────────┐
                 │  客户端 / BFF / API Gateway   │
                 └──────────────┬───────────────┘
                                │  X-API-Key / Bearer
                 ┌──────────────▼───────────────┐
                 │     customer-service-ai      │
                 │  REST + SSE + Admin(Thymeleaf)
                 │                              │
                 │  安全 · 限流                 │
                 │  路由 · 护栏 · 转人工        │
                 │  对话编排                    │
                 │  指标 · 审计 · 会话          │
                 └───┬───────────────────┬──────┘
                     │                   │
          OpenAI 兼容 LLM           知识提供方
          (本机 llama / 云端)      (Dify / 本地向量 / 无)
                     │                   │
                  PostgreSQL ◄───────────┘
                  (会话、日志、审计)
```

## 模块包结构

```
com.enterprise.csai
├── admin            # Thymeleaf 运维界面
├── audit            # 审计落库
├── chat             # 编排、会话、消息
├── common           # 配置、错误码、Schema 保障
├── domain
│   ├── policy       # 护栏、转人工策略
│   └── port         # KnowledgePort、ModelPort
├── knowledge        # Dify / local / none 检索
├── modelgateway     # 多模型注册与 OpenAI 客户端
├── observability    # Metrics 与健康指示
├── router           # 意图分类
└── security         # API Key 与限流过滤器
```

## 对话主链路

```
POST /api/v1/chat            （同步）
POST /api/v1/chat/stream     （SSE：status / delta / meta）
  → ApiKeyAuthFilter（可选）
  → RateLimitFilter
  → ChatOrchestrator
       → GuardrailPolicy（注入检测 / 依据策略）
       → RoutingService（分类模型）
       → HandoffPolicy（可选短路转人工）
       → KnowledgePort.search（需要 RAG 时）
       → ModelPort.chat / stream（回答模型）
       → 持久化会话 / 消息 / 路由 / 审计
       → 返回 answer + route + sources + degraded/handoff
```

管理台 `/admin/chat` 使用流式接口，实现 token 级输出。

## 可插拔后端

| 关注点 | 配置键 | 选项 |
|--------|--------|------|
| 模型源 | `csai.model-source` | `local`、`cloud` |
| 知识库 | `csai.knowledge.provider` | `dify`、`local`、`none` |
| 安全 | `csai.security.enabled` | `true` / `false` |

切换模型源后需**重启进程**（启动时加载 Registry）。

## 韧性与降级

- 分类、回答、Dify HTTP 均有超时  
- Dify retrieve 失败可走 segment 列表 + 关键词打分降级  
- 响应暴露 `degraded` / `degradedReasons`  
- 分类器故障降级为 `UNKNOWN`，不强制转人工  

## 数据存储

| 存储 | 用途 |
|------|------|
| PostgreSQL | 会话、消息、路由日志、审计、可选本地文档元数据 |
| PgVector | 仅当 `knowledge.provider=local` |
| Dify | 企业知识主索引 |

## 相关文档

- 产品需求：[requirements/PRD.md](requirements/PRD.md)  
- 配置：[configuration.md](configuration.md)  
- 运维：[operations/RUNBOOK.md](operations/RUNBOOK.md)  
- Dify 与模型：[development/DIFY-AND-MODELS.md](development/DIFY-AND-MODELS.md)  
