# 实现进度记忆

| 字段 | 值 |
|------|-----|
| 分支 | feat/csai-mvp |
| 状态 | **真实联调：Chat 已通；Embedding 待开通** |
| 应用 | 运行中 http://localhost:8081 （真实 glm-5.1） |

## 已验证

- Key / Base URL / glm-5.1 Chat ✅
- 闲聊「你好」→ CHITCHAT + 正常中文回复 ✅
- 路径 404 已修（compatible-mode 不去重 /v1）✅

## 待用户操作

- 百炼控制台开通 **text-embedding-v3**（或告知可用 embedding 模型名+维度）
- 开通后回复「embedding 已开通」→ 再测上传 + RAG 退款问答

## 勿泄露

- .env 含 API Key，永不提交
