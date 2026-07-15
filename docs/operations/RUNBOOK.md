# 运维手册 — customer-service-ai 0.2.0-rc

## 1. 组件依赖

| 组件 | 说明 |
|------|------|
| PostgreSQL + pgvector | 会话/审计/业务表 |
| LLM | local llama.cpp 或 cloud OpenAI 兼容 |
| Dify Dataset API | 知识检索（provider=dify） |

## 2. 配置清单

| 变量 | 生产要求 |
|------|----------|
| `CSAI_SECURITY_ENABLED` | `true` |
| `CSAI_API_KEY_CLIENT` / `CSAI_API_KEY_ADMIN` | 强随机，勿用默认 |
| `CS_AI_MODEL_SOURCE` | `local` 或 `cloud` |
| `CS_AI_CLOUD_API_KEY` | cloud 时必填 |
| `DIFY_BASE_URL` / `DIFY_API_KEY` / `DIFY_DATASET_ID` | dify 时必填 |
| DB_* | 生产密码 |

Profiles：

- `mock`：离线测试  
- 默认：本地联调（security 默认 false，可用 `CSAI_SECURITY_ENABLED=true`）  
- `prod`：强制 security  

## 3. 启动

```bash
# DB
docker compose up -d postgres

# Dify port-forward（K8s）
./scripts/dify-port-forward.sh

# 应用
set -a && source .env && set +a
export CSAI_SECURITY_ENABLED=true
mvn spring-boot:run -Dspring-boot.run.profiles=prod \
  -Dspring-boot.run.arguments=--server.port=8081
```

或：

```bash
docker compose --profile full up -d --build
```

## 4. 健康检查

```bash
curl -s http://127.0.0.1:8081/actuator/health | jq
curl -s -H "X-API-Key: $CSAI_API_KEY_CLIENT" http://127.0.0.1:8081/api/v1/runtime | jq
```

## 5. 常见故障

| 现象 | 处理 |
|------|------|
| 401 | 检查 X-API-Key / prod profile |
| 429 | 调高 `CSAI_RATE_LIMIT_RPM` 或确认刷量 |
| Dify 500 / degraded | 已知 RC bug，segment-fallback；升级 Dify |
| 模型超时 | 调 `CSAI_ANSWER_TIMEOUT_MS`；检查 llama 负载 |
| session 403 | 跨 principal 使用了他人 sessionId |

## 6. 回滚

1. 停新版本容器/进程  
2. 启上一 jar/image tag  
3. DB migration V2 向前兼容，无需回滚 DDL  

## 7. 安全注意

- 禁止在 prod 将 `csai.security.enabled=false`  
- Admin Key 仅内网  
- 定期轮换 API Key  
