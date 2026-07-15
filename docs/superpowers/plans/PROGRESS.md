# 实现进度记忆

| 字段 | 值 |
|------|-----|
| 分支 | `feat/csai-mvp` |
| 当前 | **Dify 知识库 + 本地/云端可插拔模型已落地** |
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
