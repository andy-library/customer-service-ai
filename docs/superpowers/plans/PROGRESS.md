# 实现进度记忆（断点续跑）

> **每次会话开始时先读本文件。** 完成任何任务步骤后立即更新本文件并提交。  
> 计划真源：`docs/superpowers/plans/2026-07-15-intelligent-customer-service-mvp.md`

| 字段 | 值 |
|------|-----|
| 最后更新 | 2026-07-15 |
| 当前分支 | `feat/csai-mvp` |
| 最后提交 | 见下方 git log；本文件更新后会再 commit |
| 当前任务 | **Task 6** — 对话编排 ChatOrchestrator |
| 当前步骤 | 下一步实现 chat 包（session/message/编排/API） |
| 阻塞 | 无（Testcontainers 全量 IT 需修好 Docker API 版本后补跑） |
| 规格版本 | 设计 v1.1 + 实现计划 |

---

## 如何续跑（新会话必做）

```bash
cd "/Volumes/Development HD/SourceCode/Spring AI"
git checkout feat/csai-mvp
# 1) 读进度
cat docs/superpowers/plans/PROGRESS.md
# 2) 读计划中对应 Task
# 3) 验证：
./scripts/install-framework.sh
mvn -Dtest=PomCoordinatesSanityTest,PingWebMvcTest,ModelRegistryTest,IntentJsonParserTest,RoutingServiceTest,TextExtractionServiceTest test
# 4) 从「当前任务」继续编码
```

**Docker（本机 Rancher Desktop）：**
```bash
export DOCKER_HOST=unix://$HOME/.rd/docker.sock
# Testcontainers 若报 client API 过旧，需升级 testcontainers 或 docker 客户端后再跑 ApiResponseContractTest
```

---

## 任务总览

| Task | 名称 | 状态 | 备注 |
|------|------|------|------|
| 1 | 框架安装 + Maven 骨架 + Compose | ✅ done | d22f783 起 |
| 2 | CsaiProperties + Flyway + ApiResponse | ✅ done | Ping 用对象包装 |
| 3 | 模型网关 | ✅ done | 7b581bb |
| 4 | LLM 意图路由 | ✅ done | 同 7b581bb 含 router |
| 5 | 知识库 RAG | ✅ done（单元） | 入库/检索代码已齐；IT 待 Docker |
| 6 | 对话编排 | 🔄 **下一步** | |
| 7 | SSE 流式 | ⏳ pending | |
| 8 | Admin UI | ⏳ pending | |
| 9 | mock profile + README | ⏳ pending | |
| 10 | 验收 | ⏳ pending | |

---

## 已实现代码地图

```
src/main/java/com/enterprise/csai/
  CustomerServiceAiApplication.java
  common/{api/PingController, config/CsaiProperties, error/CsaiErrorCodes}
  modelgateway/{ModelRole, ModelView, ModelRegistry, ModelGateway, ModelGatewayService,
                ModelGatewayConfiguration, api/ModelController}
  router/{IntentType, ClassificationResult, IntentJsonParser, RoutingDecision,
          RoutingService, api/RouterController}
  knowledge/{Document*, TextExtractionService, DocumentIngestService,
             KnowledgeSearchService, KnowledgeChunk, api/KnowledgeController}
src/main/resources/
  application.yml  (含 3 个默认模型 + 意图映射)
  db/migration/V1__init_csai.sql
samples/refund-policy.md
```

## 验证命令（当前应全绿）

```bash
mvn -Dtest=PomCoordinatesSanityTest,PingWebMvcTest,ModelRegistryTest,IntentJsonParserTest,RoutingServiceTest,TextExtractionServiceTest test
# 期望: Tests run: 15, Failures: 0
```

## 关键技术债 / 约定

1. **starter-parent BOM**：`install-framework.sh` patch `${project.version}` → `1.0.0-alpha.1`  
2. **enforcer enforce-versions**：业务 pom `phase=none`（Spring AI 1.1.8 vs Boot 3.3 Jackson）  
3. **database-starter**：排除 `shardingsphere-jdbc`  
4. **ApiResponse**：裸 `String` 不包装，Controller 须返回对象  
5. **向量删除**：按 metadata `documentId` filter；失败仅 warn  
6. **TokenTextSplitter**：用 builder `withChunkSize` / `withMinChunkSizeChars`（无独立 overlap 参数）  
7. **全量 SpringBootTest + pgvector**：待修复 Testcontainers/Docker API  

## 会话日志

| 时间 | 事件 |
|------|------|
| 2026-07-15 | 创建进度记忆；开始实现 |
| 2026-07-15 | Task1–2 完成 d22f783 |
| 2026-07-15 | Task3–4 完成 7b581bb |
| 2026-07-15 | Task5 知识库代码 + 单测通过；下一会话做 Task6 对话编排 |
