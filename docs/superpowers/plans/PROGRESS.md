# 实现进度记忆

| 字段 | 值 |
|------|-----|
| 分支 | `feat/csai-mvp` |
| 当前 | **E2E 冒烟已通过：local-qwen + Dify 知识库** |
| 机器 | Mac M1 64G；本机 llama Chat **:18080** `local-qwen`，Embed **:18081** `local-bge-m3` (dim 1024) |
| 知识库 | **Dify**（`csai.knowledge.provider=dify`）；Spring 只调用 Dataset retrieve |
| 模型切换 | `CS_AI_MODEL_SOURCE=local\|cloud`（启动时解析，改后重启） |

## 架构（已实现）

```
用户 → Spring AI（路由/会话/编排）
         ├─ model-source=local  → llama :18080 (local-qwen)
         ├─ model-source=cloud  → 百炼 compatible-mode（csai.cloud.* 可插拔）
         └─ knowledge.provider=dify → Dify Dataset Retrieve API
```

## 用户下一步

1. 确认本机 llama 已起：
   ```bash
   ./scripts/local-llm/healthcheck.sh
   ```
2. 配置 Dify 与 `.env`：
   ```bash
   cp .env.local-llama.example .env
   # 填写 DIFY_BASE_URL / DIFY_API_KEY / DIFY_DATASET_ID
   # 本地模型保持 CS_AI_MODEL_SOURCE=local
   ./scripts/check-env.sh
   ```
3. 起 Postgres + 应用：
   ```bash
   docker compose up -d postgres
   set -a && source .env && set +a
   mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
   ```
4. 验收：
   - `GET http://localhost:8081/api/v1/runtime` → modelSource / knowledgeProvider
   - Admin `/admin/knowledge` 检索调试
   - Admin `/admin/chat` 或 `POST /api/v1/chat`
5. 切云端（可选）：
   ```bash
   # .env
   CS_AI_MODEL_SOURCE=cloud
   CS_AI_CLOUD_API_KEY=sk-...
   # 重启应用
   ```

## 文档

| 文档 | 路径 |
|------|------|
| **Dify + 可插拔模型** | `docs/development/DIFY-知识库与可插拔模型.md` |
| 本地 llama 全量部署 | `docs/development/LOCAL-LLAMA-全量部署-M1.md` |
| 百炼 glm | `docs/development/BAILIAN-GLM-配置.md` |
| 开发文档 | `docs/development/DEV-智能客服-MVP.md` |

## 最近完成

- [x] KnowledgeRetriever 三态：dify / local / none
- [x] DifyKnowledgeRetriever + Admin 检索调试
- [x] ActiveModelProfileResolver local/cloud
- [x] application.yml 默认对齐 18080/18081 + local-qwen / local-bge-m3
- [x] RuntimeConfigController `/api/v1/runtime`
- [x] 单测：Dify 解析、ActiveModelProfileResolver
- [x] E2E 冒烟：knowledge search + chat（intent=BILLING, rag, sources, 正确回答退款政策）
- [x] Dify 1.16.0-rc1 `/retrieve` 有命中时 500 → 已实现 **segment-fallback** 降级
- [x] `scripts/dify-port-forward.sh` + `scripts/smoke-e2e.sh`

## 已知问题

| 问题 | 处理 |
|------|------|
| Dify 1.16.0-rc1 Dataset retrieve：向量命中后序列化抛 SQLAlchemy closed transaction | Spring 侧 `segment-fallback=true`（默认）；升级 Dify 稳定版后可关 |
| 知识库原为空 | 已通过 API 导入 `samples/refund-policy.md` 作为冒烟语料 |
