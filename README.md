# Customer Service AI (MVP)

企业智能客服 MVP：基于 [microservice-framework](https://github.com/andy-library/microservice-framework) + Spring AI。

## 技术栈

| 组件 | 版本 |
|------|------|
| OpenJDK | 21 |
| microservice-framework | 1.0.0-alpha.1（Boot 3.3.13 / Cloud 2023.0.6） |
| Spring AI | 1.1.8 |
| DB | PostgreSQL + PgVector |

## 快速开始

### 1. 安装技术底座（首次）

```bash
./scripts/install-framework.sh
```

### 2. 启动数据库

```bash
# Rancher Desktop 用户如需：
# export DOCKER_HOST=unix://$HOME/.rd/docker.sock
docker compose up -d
```

### 3. 配置密钥（真实模型）

```bash
cp .env.example .env
# 编辑 .env 填入 OpenAI-compatible base-url / api-key
set -a && source .env && set +a
mvn spring-boot:run
```

### 4. 无密钥演示（mock）

```bash
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=mock
```

mock 使用确定性分类/回答与内存向量库，仍依赖 Postgres 存业务表。

### 5. 访问

| 地址 | 说明 |
|------|------|
| http://localhost:8080/admin | 管理后台 |
| http://localhost:8080/admin/chat | 对话测试台 |
| http://localhost:8080/api/v1/ping | 探活（ApiResponse 包装） |
| http://localhost:8080/api/v1/models | 模型列表 |
| http://localhost:8080/api/v1/chat | 同步问答 |
| http://localhost:8080/swagger-ui.html | OpenAPI |
| http://localhost:8080/actuator/health | 健康检查 |

### API 示例

```bash
curl -s -X POST http://localhost:8080/api/v1/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"如何申请退款？"}' | jq

curl -s -X POST http://localhost:8080/api/v1/knowledge/documents \
  -F 'file=@./samples/refund-policy.md' \
  -F 'title=退款政策'
```

## 测试

```bash
mvn test
```

## 断点续跑

中断后请先读：

**`docs/superpowers/plans/PROGRESS.md`**

然后按「当前任务」继续。实现计划见  
`docs/superpowers/plans/2026-07-15-intelligent-customer-service-mvp.md`。

## 文档索引

| 文档 | 路径 |
|------|------|
| 进度记忆 | `docs/superpowers/plans/PROGRESS.md` |
| 实现计划 | `docs/superpowers/plans/2026-07-15-intelligent-customer-service-mvp.md` |
| 设计 v1.1 | `docs/superpowers/specs/2026-07-15-intelligent-customer-service-design.md` |
| 底座分析 | `docs/analysis/microservice-framework-foundation-analysis.md` |
| PRD | `docs/requirements/PRD-智能客服-MVP.md` |
| 开发文档 | `docs/development/DEV-智能客服-MVP.md` |
| 验收清单 | `docs/acceptance/ACCEPTANCE-智能客服-MVP.md` |

## 模块结构

```
com.enterprise.csai
  common / modelgateway / router / knowledge / chat / admin
```

主链路：提问 → LLM 意图分类 → 选模型 →（可选）RAG → 回答 + sources。
