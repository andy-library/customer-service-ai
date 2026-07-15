# 实现进度记忆（断点续跑）

> **每次会话开始时先读本文件。**  
> 计划：`docs/superpowers/plans/2026-07-15-intelligent-customer-service-mvp.md`

| 字段 | 值 |
|------|-----|
| 最后更新 | 2026-07-15 |
| 当前分支 | `feat/csai-mvp` |
| 当前任务 | **Task 10 验收收尾（基本完成）** / 可选优化 |
| 状态 | MVP 主链路已可运行（mock profile） |
| 阻塞 | 无 |

---

## 任务总览

| Task | 状态 |
|------|------|
| 1 工程骨架 + 框架 | ✅ |
| 2 Properties + Flyway + ApiResponse | ✅ |
| 3 模型网关 | ✅ |
| 4 意图路由 | ✅ |
| 5 知识库 RAG | ✅ |
| 6 对话编排 | ✅ |
| 7 SSE 流式 | ✅（API 已实现） |
| 8 Admin UI | ✅ |
| 9 mock profile + README | ✅ |
| 10 验收 smoke | ✅ mock 端到端已通 |

---

## 本地运行（当前验证通过）

```bash
./scripts/install-framework.sh
export DOCKER_HOST=unix://$HOME/.rd/docker.sock   # Rancher Desktop
docker compose up -d
# 若 8080 被占用可用 8081
mvn spring-boot:run -Dspring-boot.run.profiles=mock \
  -Dspring-boot.run.arguments=--server.port=8081
```

访问：http://localhost:8081/admin  

### 已验证 smoke

- `GET /api/v1/ping` → ApiResponse  
- `GET /api/v1/models` → 3 个模型  
- 上传 `samples/refund-policy.md` → INDEXED  
- `POST /api/v1/chat`「如何申请退款？」→ intent=BILLING, answer-strong, **sources 非空**  
- `/admin` → 200  

### 单测

```bash
mvn -Dtest=ChatOrchestratorTest,TextExtractionServiceTest,RoutingServiceTest,ModelRegistryTest,PingWebMvcTest,PomCoordinatesSanityTest,IntentJsonParserTest test
# 18 tests green
```

---

## 关键修复备忘

1. `framework.common.id.worker-id` **必须配置**（默认 1）  
2. Flyway：框架可能不自动 migrate → `FlywayMigrationConfig` 启动时 `flyway.migrate()`；也可手跑 V1 SQL  
3. 裸 String 不包装 ApiResponse → DTO/record  
4. mock 向量检索支持中文 bigram  
5. `install-framework.sh` patch starter-parent BOM `${project.version}`  
6. enforcer enforce-versions phase=none（Jackson 冲突）  

---

## 续跑建议（若还要做）

- 真实模型 key 联调（非 mock）  
- 修 Testcontainers Docker API 版本后补 `ApiResponseContractTest`  
- SSE 手工 curl 验证  
- 按 `docs/acceptance/ACCEPTANCE-智能客服-MVP.md` 正式勾选  

## 会话日志

| 时间 | 事件 |
|------|------|
| 2026-07-15 | Task 6–9 完成；mock 端到端 chat+RAG+admin 通过 |
