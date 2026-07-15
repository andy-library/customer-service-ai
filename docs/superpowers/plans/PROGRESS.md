# 实现进度记忆

| 字段 | 值 |
|------|-----|
| 分支 | feat/csai-mvp |
| 当前 | **全本地 llama 部署文档与脚本已就绪** |
| 机器 | Mac M1 64G；模型在 /Users/andy.yang/LocalModels |
| Chat 推荐 | Qwen3.6-35B-A3B IQ2_M → :8082 alias qwen3.6-35b |
| Embed | 需下载 Qwen3-Embedding-0.6B 或 bge-m3 → :8083 |

## 用户下一步

1. 下载 embedding GGUF 到 LocalModels
2. ./scripts/local-llm/start-chat.sh && start-embed.sh
3. ./scripts/local-llm/healthcheck.sh
4. 用 .env.local-llama.example 覆盖/合并 .env
5. 启动 Spring 非 mock 验收

详见 docs/development/LOCAL-LLAMA-全量部署-M1.md
