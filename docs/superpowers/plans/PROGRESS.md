# 实现进度记忆（断点续跑）

> **每次会话开始时先读本文件。** 完成任何任务步骤后立即更新本文件并提交。  
> 计划真源：`docs/superpowers/plans/2026-07-15-intelligent-customer-service-mvp.md`

| 字段 | 值 |
|------|-----|
| 最后更新 | 2026-07-15 |
| 当前分支 | `feat/csai-mvp` |
| 最后提交 | `d22f783` scaffold + ping contract |
| 当前任务 | **Task 3** — 模型网关 |
| 当前步骤 | 即将实现 ModelRegistry / ModelGateway |
| 阻塞 | 无 |
| 规格版本 | 设计 v1.1 + 实现计划 |

---

## 如何续跑

1. 读本文件 + 实现计划  
2. 从「当前任务 / 当前步骤」继续  
3. 完成 Step 后更新本文件并 commit  
4. Docker（本机）：`export DOCKER_HOST=unix://$HOME/.rd/docker.sock`（Rancher Desktop）  
   Testcontainers 若报 API version 过旧，需升级 testcontainers 或 Docker 客户端  

---

## 任务总览

| Task | 名称 | 状态 |
|------|------|------|
| 1 | 框架安装脚本 + Maven 骨架 + Compose | ✅ done |
| 2 | CsaiProperties + Flyway + ApiResponse 契约 | ✅ done |
| 3 | 模型网关 ModelRegistry/Gateway | 🔄 in_progress |
| 4 | LLM 意图路由 | ⏳ pending |
| 5 | 知识库入库 + PgVector 检索 | ⏳ pending |
| 6 | 对话编排（同步 + 会话） | ⏳ pending |
| 7 | SSE 流式对话 | ⏳ pending |
| 8 | Thymeleaf 管理后台 | ⏳ pending |
| 9 | 默认模型配置 + mock profile + README | ⏳ pending |
| 10 | 端到端验收 + 文档收尾 | ⏳ pending |

---

## Task 1–2 已完成要点

- parent: microservice-framework-starter-parent 1.0.0-alpha.1  
- install-framework.sh patch BOM `${project.version}`  
- enforcer enforce-versions phase=none（Jackson 冲突）  
- exclude shardingsphere  
- Flyway V1 业务表  
- Ping 返回对象以触发 ApiResponse 包装（裸 String 不包装）  
- 测试：`PomCoordinatesSanityTest` + `PingWebMvcTest` 通过  
- `ApiResponseContractTest` 需可用 Testcontainers（当前常 skip）  

## Task 3 清单

- [ ] ModelView / ModelRegistry / ModelGateway  
- [ ] ModelGatewayConfiguration（多 OpenAI-compatible ChatModel）  
- [ ] ModelController GET /api/v1/models  
- [ ] ModelRegistryTest  
- [ ] Commit  

---

## 关键决策 / 技术债

见历史：BOM patch、enforcer skip、ShardingSphere 排除、裸 String 不包装。

## 会话日志

| 时间 | 事件 |
|------|------|
| 2026-07-15 | Task 1–2 完成并提交 d22f783；进入 Task 3 |
