# 实现进度记忆（断点续跑）

> **每次会话开始时先读本文件。** 完成任何任务步骤后立即更新本文件并提交。  
> 计划真源：`docs/superpowers/plans/2026-07-15-intelligent-customer-service-mvp.md`

| 字段 | 值 |
|------|-----|
| 最后更新 | 2026-07-15 |
| 当前分支 | `feat/csai-mvp` |
| 当前任务 | **Task 2** — CsaiProperties + Flyway + ApiResponse 契约 |
| 当前步骤 | 即将开始 Task 2 Step 1 |
| 阻塞 | 无 |
| 规格版本 | 设计 v1.1 + 实现计划 |

---

## 如何续跑（新会话复制这段）

1. 打开并阅读本文件 `docs/superpowers/plans/PROGRESS.md`  
2. 打开计划 `docs/superpowers/plans/2026-07-15-intelligent-customer-service-mvp.md`  
3. 从「当前任务 / 当前步骤」继续，**不要**重做已勾选完成项  
4. 每完成一个 Step：勾选下方清单 → 更新「当前任务/步骤」→ `git commit`  
5. 若中断：在「会话日志」追加一行说明停在何处  

---

## 任务总览

| Task | 名称 | 状态 |
|------|------|------|
| 1 | 框架安装脚本 + Maven 骨架 + Compose | ✅ done |
| 2 | CsaiProperties + Flyway + ApiResponse 契约 | 🔄 in_progress |
| 3 | 模型网关 ModelRegistry/Gateway | ⏳ pending |
| 4 | LLM 意图路由 | ⏳ pending |
| 5 | 知识库入库 + PgVector 检索 | ⏳ pending |
| 6 | 对话编排（同步 + 会话） | ⏳ pending |
| 7 | SSE 流式对话 | ⏳ pending |
| 8 | Thymeleaf 管理后台 | ⏳ pending |
| 9 | 默认模型配置 + mock profile + README | ⏳ pending |
| 10 | 端到端验收 + 文档收尾 | ⏳ pending |

状态图例：⏳ pending · 🔄 in_progress · ✅ done · ⚠️ blocked

---

## Task 1 清单

- [x] Step 1: `scripts/install-framework.sh`
- [x] Step 2: 执行 install（本地 m2 已有 + patch BOM）
- [x] Step 3: `pom.xml`
- [x] Step 4: Application + yml + compose + .env.example + .gitignore
- [x] Step 5: `PomCoordinatesSanityTest`
- [x] Step 6: `mvn test` PASS（2 tests）
- [x] Step 7: Commit（本提交）

## Task 2 清单

- [ ] Step 1: 契约测试 ApiResponseContractTest
- [ ] Step 2: 先跑失败
- [ ] Step 3: CsaiProperties + Flyway V1 + PingController
- [ ] Step 4: 契约测试通过
- [ ] Step 5: Commit

## Task 3–10

- [ ] Task 3 全部完成  
- [ ] Task 4 全部完成  
- [ ] Task 5 全部完成  
- [ ] Task 6 全部完成  
- [ ] Task 7 全部完成  
- [ ] Task 8 全部完成  
- [ ] Task 9 全部完成  
- [ ] Task 10 全部完成  

---

## 关键决策备忘（实现期锁定）

- Parent: `microservice-framework-starter-parent:1.0.0-alpha.1`
- Spring Boot: **3.3.13**（禁止覆盖）
- Spring AI: **1.1.8**
- 主键: UUID
- 业务包: `com.enterprise.csai.**`
- DB: PostgreSQL + PgVector + Flyway
- 响应: 框架 `ApiResponse`
- **install-framework.sh** 会 patch starter-parent 中 `${project.version}` BOM 导入为 `1.0.0-alpha.1`
- **Enforcer** `enforce-versions` 对本业务应用 `phase=none`（Spring AI 1.1.8 Jackson 与 Boot 3.3 上界冲突）
- **database-starter** 排除 `shardingsphere-jdbc`（MVP 单库）

## 已知问题 / 技术债

1. Framework starter-parent 用 `${project.version}` 导入 BOM → 消费方版本污染；脚本已 patch 本地 m2。  
2. Spring AI 1.1.8 vs Boot 3.3.13 Jackson upper-bound → 业务 pom 关闭 enforcer 该 execution。  
3. 全量 `@SpringBootTest` 需 Docker/pgvector（Task 2 起）。

## 会话日志

| 时间 | 事件 |
|------|------|
| 2026-07-15 | 创建进度记忆；开始 Task 1 |
| 2026-07-15 | Task 1 完成：骨架可构建；进入 Task 2 |
