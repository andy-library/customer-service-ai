# Customer Service AI (MVP)

企业智能客服 MVP：基于 [microservice-framework](https://github.com/andy-library/microservice-framework) + Spring AI。

## 技术栈

- OpenJDK 21
- microservice-framework `1.0.0-alpha.1`（Spring Boot 3.3.13 / Cloud 2023.0.6）
- Spring AI `1.1.8`
- PostgreSQL + PgVector

## 快速开始

### 1. 安装技术底座（首次必须）

```bash
./scripts/install-framework.sh
```

### 2. 启动数据库

```bash
docker compose up -d
```

### 3. 配置密钥

```bash
cp .env.example .env
# 编辑 .env，填入 OpenAI-compatible 模型 endpoint 与 API Key
```

### 4. 运行应用

```bash
# 加载 .env 后启动（示例）
set -a && source .env && set +a
mvn spring-boot:run
```

无密钥时可使用 mock profile（完善于后续任务）：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mock
```

### 5. 访问

| 地址 | 说明 |
|------|------|
| http://localhost:8080/admin | 管理后台（Task 8） |
| http://localhost:8080/api/v1/ping | 健康探活（Task 2） |
| http://localhost:8080/swagger-ui.html | OpenAPI |
| http://localhost:8080/actuator/health | Actuator |

## 文档

| 文档 | 路径 |
|------|------|
| **断点进度（续跑必读）** | `docs/superpowers/plans/PROGRESS.md` |
| 实现计划 | `docs/superpowers/plans/2026-07-15-intelligent-customer-service-mvp.md` |
| 设计规格 v1.1 | `docs/superpowers/specs/2026-07-15-intelligent-customer-service-design.md` |
| 底座分析 | `docs/analysis/microservice-framework-foundation-analysis.md` |
| PRD | `docs/requirements/PRD-智能客服-MVP.md` |
| 开发文档 | `docs/development/DEV-智能客服-MVP.md` |
| 验收清单 | `docs/acceptance/ACCEPTANCE-智能客服-MVP.md` |

## 断点续跑

中断后请先读 `docs/superpowers/plans/PROGRESS.md`，从「当前任务 / 当前步骤」继续。
