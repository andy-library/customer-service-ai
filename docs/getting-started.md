# Getting Started

Author: **andy yang**

This guide gets **customer-service-ai** running on your machine.

## 1. Prerequisites

| Tool | Version |
|------|---------|
| OpenJDK | 21+ |
| Maven | 3.9+ |
| Docker | 20+ (for PostgreSQL) |
| Optional | [llama.cpp](https://github.com/ggerganov/llama.cpp) `llama-server` |
| Optional | [Dify](https://dify.ai/) for knowledge base |

## 2. Clone

```bash
git clone https://github.com/andy-library/customer-service-ai.git
cd customer-service-ai
```

> Replace the GitHub org/user with your fork if needed.

## 3. Install framework parent (first time)

This project inherits `microservice-framework-starter-parent`.

```bash
./scripts/install-framework.sh
```

If the parent is already in your local Maven repository, you can skip this step.

## 4. Start PostgreSQL

```bash
docker compose up -d postgres
```

## 5. Configure

```bash
cp .env.example .env
# or for local llama defaults:
# cp .env.local-llama.example .env
```

Minimum variables:

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=csai
DB_USER=csai
DB_PASSWORD=csai

# Models (local example)
CS_AI_MODEL_SOURCE=local
CS_AI_DEFAULT_BASE_URL=http://127.0.0.1:18080/v1
CS_AI_DEFAULT_API_KEY=sk-local
CS_AI_CLASSIFIER_MODEL=local-qwen
CS_AI_ANSWER_STRONG_MODEL=local-qwen
CS_AI_ANSWER_FAST_MODEL=local-qwen

# Knowledge
CS_AI_KNOWLEDGE_PROVIDER=dify   # or local | none
DIFY_BASE_URL=http://127.0.0.1:15001/v1
DIFY_API_KEY=
DIFY_DATASET_ID=

# Security (recommended for shared environments)
CSAI_SECURITY_ENABLED=false
CSAI_API_KEY_CLIENT=change-me-client
CSAI_API_KEY_ADMIN=change-me-admin
```

Validate:

```bash
./scripts/check-env.sh
```

## 6. Run (offline mock — no LLM keys)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mock \
  -Dspring-boot.run.arguments=--server.port=8081
```

## 7. Run (real models)

1. Start OpenAI-compatible chat (and optionally embedding) servers.
2. Start Dify (if `knowledge.provider=dify`) and create a dataset + API key.
3. Fill `.env` and:

```bash
set -a && source .env && set +a
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Local llama helper scripts (optional):

```bash
# Requires MODEL_DIR or MODEL path to GGUF files
export MODEL_DIR="$HOME/LocalModels"
./scripts/local-llm/start-chat.sh
./scripts/local-llm/start-embed.sh   # only if knowledge.provider=local
./scripts/local-llm/healthcheck.sh
```

## 8. Verify

```bash
# Health
curl -s http://127.0.0.1:8081/actuator/health | jq

# Ping
curl -s http://127.0.0.1:8081/api/v1/ping | jq

# Chat (when security is off)
curl -s -X POST http://127.0.0.1:8081/api/v1/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"Hello"}' | jq

# Chat (when security is on)
curl -s -X POST http://127.0.0.1:8081/api/v1/chat \
  -H 'Content-Type: application/json' \
  -H "X-API-Key: $CSAI_API_KEY_CLIENT" \
  -d '{"message":"Hello"}' | jq
```

Admin UI: http://127.0.0.1:8081/admin  
OpenAPI: http://127.0.0.1:8081/swagger-ui.html

## 9. Tests

```bash
mvn test
```

## Next

- [architecture.md](architecture.md)
- [configuration.md](configuration.md)
- [operations/RUNBOOK.md](operations/RUNBOOK.md)
