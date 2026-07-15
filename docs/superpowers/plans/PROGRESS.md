# 实现进度记忆（断点续跑）

| 字段 | 值 |
|------|-----|
| 最后更新 | 2026-07-15 |
| 当前分支 | `feat/csai-mvp` |
| 当前任务 | **真实模型联调 — 等待用户配置 .env** |
| 当前步骤 | 联调工具已就绪；需用户填写 OpenAI-compatible Key 后执行 run-real + smoke |
| 阻塞 | 仓库内无 `.env`；本机环境仅见 ANTHROPIC_API_KEY（**非** OpenAI 兼容，本 MVP 不能直接用） |

## 已完成

- MVP Task 1–10（mock 端到端）✅  
- 真实联调工具：
  - `scripts/check-env.sh` / `run-real.sh` / `smoke-real.sh`
  - `EmbeddingModelConfiguration`（`!mock`，使用 `csai.embedding`）
  - `docs/development/REAL-MODEL-联调.md`

## 用户操作（继续联调）

```bash
cp .env.example .env
# 编辑 .env 填入 OpenAI-compatible：
#   CS_AI_DEFAULT_BASE_URL
#   CS_AI_DEFAULT_API_KEY
#   以及可用的 chat / embedding 模型名
./scripts/check-env.sh
./scripts/run-real.sh
# 另开终端：
./scripts/smoke-real.sh http://localhost:8081
```

填好 `.env` 后回复「已配置」或「继续」，我会代为执行 check + 启动 + smoke（**不会**在日志中回显密钥）。

## 会话日志

| 时间 | 事件 |
|------|------|
| 2026-07-15 | 选项1：真实模型联调；完成工具与文档；等待 .env |
