# 验收清单

| 字段 | 值 |
|------|-----|
| 应用版本 | `0.2.0-rc.1` |
| 需求 | [requirements/PRD.md](../requirements/PRD.md) |
| 作者 | andy yang |

用于发版或部署前勾选验证。

## 安全

- [ ] `CSAI_SECURITY_ENABLED=true` 时无 Key 请求 → **401**  
- [ ] 错误 Key → **401**  
- [ ] 合法客户端 Key → 对话 **200**  
- [ ] 客户端 Key 访问 `/admin` → **403**  
- [ ] 管理员 Key 访问 `/admin` → **200**  
- [ ] 主体 A 的会话不能被主体 B 续聊 → 拒绝  

## 对话与流式

- [ ] `POST /api/v1/chat` 返回 `route` + `answer`  
- [ ] `POST /api/v1/chat/stream` 输出 `status` / `delta` / `meta`  
- [ ] 管理台 **对话测试** 页面可流式显示 token  
- [ ] RAG 场景命中时返回 `sources`  
- [ ] 策略触发时出现 `degraded` / `handoff`  

## 知识与模型

- [ ] `provider=dify` 可检索（或降级 fallback）  
- [ ] `provider=none`（mock）不检索  
- [ ] `CS_AI_MODEL_SOURCE=local` 注册 local-qwen（或配置别名）  
- [ ] 切到 `cloud` 后重启使用 `csai.cloud.*`  

## 运维

- [ ] `GET /actuator/health` → UP（DB 可用）  
- [ ] `GET /api/v1/runtime` 显示 modelSource 与 knowledgeProvider  
- [ ] `mvn test` 通过  
- [ ] 运维手册与实际部署步骤一致  
- [ ] 无密钥入库（`.env` 已忽略；示例为空）  

## 冒烟命令

```bash
curl -s http://127.0.0.1:8081/actuator/health
curl -s http://127.0.0.1:8081/api/v1/ping
curl -s -X POST http://127.0.0.1:8081/api/v1/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"你好"}'

# 开启安全时：
curl -s -X POST http://127.0.0.1:8081/api/v1/chat \
  -H 'Content-Type: application/json' \
  -H "X-API-Key: $CSAI_API_KEY_CLIENT" \
  -d '{"message":"你好"}'
```
