# 变更记录

本文档记录本项目的重要变更。

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

> English notes (brief): see release pages on GitHub.

## [0.2.0-rc.1] — 2026-07-15

### 新增

- 准生产安全：API Key 鉴权（`X-API-Key` / Bearer）、Admin 角色  
- 滑动窗口限流，超限 HTTP 429  
- 会话归属（`owner_id`）与跨主体防护  
- 模型 / Dify 超时与有界流式线程池  
- 护栏（系统提示、依据策略、基础注入检测）  
- 转人工占位（`handoff` / `handoffReason`）  
- 知识检索降级标记（`degraded` / `degradedReasons`）  
- 审计表与 Micrometer 业务指标  
- 健康检查明细  
- Docker 打包、运维手册、冒烟脚本  
- 管理台 **SSE 流式对话测试**  
- 开源中文文档体系  

### 变更

- 默认知识提供方：Dify Dataset API  
- 可插拔模型源：`local` / `cloud`  
- 版本进入准生产 RC  

### 已知限制

- 部分 Dify 1.16.x RC 在 Dataset `/retrieve` 有命中后可能 500；默认开启 segment 降级  
- 管理台在开启安全时需请求头携带 API Key  
- 完整工单、多渠道、SaaS 多租户不在本版本范围  

## [0.1.0] — 2026-07-15

### 新增

- 基于 microservice-framework + Spring AI 的模块化单体 MVP  
- 意图路由、多模型网关、对话编排（同步 + SSE 后端）  
- 可选本地 PgVector 入库  
- Admin Thymeleaf 控制台  
- mock 离线配置  

[0.2.0-rc.1]: https://github.com/andy-library/customer-service-ai/releases/tag/v0.2.0-rc.1
[0.1.0]: https://github.com/andy-library/customer-service-ai/tree/main
