# 实现进度记忆（断点续跑）

| 字段 | 值 |
|------|-----|
| 最后更新 | 2026-07-15 |
| 当前分支 | `feat/csai-mvp` |
| 当前任务 | **百炼联调：.env 非密钥项已补全** |
| 当前步骤 | 等待用户填写 `CS_AI_DEFAULT_API_KEY` 后启动 smoke |
| 阻塞 | 仅差百炼 API Key |

## 用户待填

- [ ] `CS_AI_DEFAULT_API_KEY` = 百炼 API Key（可与 CC Switch Ali-GLM 5.1 相同）

## 已由助手写入 .env

- Base URL: `https://dashscope.aliyuncs.com/compatible-mode/v1`
- Chat models: `glm-5.1` × 3 角色
- Embedding: `text-embedding-v3` / dim `1024`
- DB / worker / port 8081

## 下一步

用户填好 Key 后回复「已填 Key」→ `./scripts/check-env.sh` + `run-real.sh` + `smoke-real.sh`
