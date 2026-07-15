# 真实模型联调指南

## 前提

- 已完成 mock 主链路（见 `PROGRESS.md`）
- 具备 **OpenAI-compatible** 的 Chat + Embedding 服务  
  - 支持：OpenAI、DeepSeek、通义 compatible-mode、智谱 OpenAI 协议、自建网关等  
  - **不支持**：Anthropic Claude 原生 API（协议不同；需兼容网关转换）
- PostgreSQL + pgvector 已启动（`docker compose up -d`）

## 步骤

### 1. 配置密钥

```bash
cp .env.example .env
# 编辑 .env，至少设置：
#   CS_AI_DEFAULT_BASE_URL
#   CS_AI_DEFAULT_API_KEY
# 以及可用的 model 名称（分类 / 回答 / embedding）
```

校验（不打印密钥内容）：

```bash
./scripts/check-env.sh
```

### 2. 启动（真实模型，非 mock）

```bash
./scripts/run-real.sh
# 默认端口 8081；可 SERVER_PORT=8080 ./scripts/run-real.sh
```

或手动：

```bash
set -a && source .env && set +a
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

**不要**加 `spring.profiles.active=mock`。

### 3. Smoke

另开终端：

```bash
./scripts/smoke-real.sh http://localhost:8081
```

期望：

| 步骤 | 期望 |
|------|------|
| ping | `code=0` |
| models | ≥2 个模型，无 apiKey |
| 上传退款政策 | status=INDEXED |
| 你好 | intent 多为 CHITCHAT，answer 非空 |
| 如何申请退款 | intent 多为 BILLING，**sources 最好非空**，答案引用政策 |

管理台：http://localhost:8081/admin

## 常见问题

| 现象 | 处理 |
|------|------|
| 401 / invalid api key | 检查 DEFAULT_API_KEY 与 base-url 是否匹配厂商 |
| embedding 维度错误 | 调整 `CS_AI_EMBEDDING_DIMENSIONS` 与模型一致；可能需重建向量表 |
| 分类总是 UNKNOWN | 看日志 classifier 原始输出；确认 CLASSIFIER 模型可用 |
| 只有 mock 能跑 | 确认未启用 `mock` profile；`ModelGatewayConfiguration` 仅在 `!mock` 生效 |
| Anthropic Key | 本 MVP 不直接接入；请用 OpenAI 兼容端点 |

## 安全

- `.env` 已在 `.gitignore`，禁止提交  
- 日志经 framework masking；仍避免在日志中打印完整 key  
