# customer-service-ai

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.x-green.svg)](https://spring.io/projects/spring-ai)

**Enterprise intelligent customer-service orchestration** built with Spring AI.

Author & maintainer: **andy yang**  
License: **Apache License 2.0**

This application is the **orchestration layer** for AI customer service:

- Intent classification and multi-model routing  
- Knowledge retrieval via **Dify** (primary) or optional local PgVector  
- Pluggable **local** (llama.cpp / OpenAI-compatible) and **cloud** LLM backends  
- Quasi-production features: API keys, rate limits, timeouts, guardrails, handoff placeholder, audit & metrics  

> Knowledge authoring lives in **Dify**. This service **calls** Dify retrieve APIs and generates answers.

---

## Table of contents

- [Features](#features)
- [Architecture](#architecture)
- [Quick start](#quick-start)
- [Configuration](#configuration)
- [API examples](#api-examples)
- [Security](#security)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [License](#license)

---

## Features

| Area | Capability |
|------|------------|
| Routing | LLM intent classification → model selection |
| Knowledge | Dify Dataset retrieve / local vector / none |
| Models | `local` or `cloud` OpenAI-compatible endpoints |
| Chat | Sync JSON + SSE streaming |
| Safety | Optional API Key auth, rate limit, session ownership |
| Quality | Guardrails, evidence policy, handoff placeholder |
| Ops | Health, Micrometer metrics, audit log, Docker |

**Version:** `0.2.0-rc.1` (quasi-production release candidate)

---

## Architecture

```
Client ──► customer-service-ai
              ├─ Model source: local llama.cpp OR cloud OpenAI-compatible
              ├─ Knowledge: Dify Dataset API (default) | local PgVector | none
              └─ PostgreSQL: sessions, route logs, audit
```

See [docs/architecture.md](docs/architecture.md) for the full design.

---

## Quick start

### Prerequisites

- OpenJDK **21**
- Maven **3.9+**
- Docker (PostgreSQL)
- Optional: llama.cpp `llama-server`, Dify

### 1) Install framework parent (first time)

```bash
./scripts/install-framework.sh
```

### 2) Start database

```bash
docker compose up -d postgres
```

### 3) Configure

```bash
cp .env.example .env
# edit secrets / endpoints — never commit .env
```

### 4) Offline demo (no external LLM)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mock \
  -Dspring-boot.run.arguments=--server.port=8081
```

### 5) Real models

Point `CS_AI_*` at your OpenAI-compatible chat endpoint and configure Dify if needed:

```bash
set -a && source .env && set +a
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

| URL | Description |
|-----|-------------|
| http://localhost:8081/actuator/health | Health |
| http://localhost:8081/api/v1/ping | Ping |
| http://localhost:8081/admin | Admin UI |
| http://localhost:8081/swagger-ui.html | OpenAPI |

More detail: [docs/getting-started.md](docs/getting-started.md)

---

## Configuration

| Variable | Meaning |
|----------|---------|
| `CS_AI_MODEL_SOURCE` | `local` \| `cloud` |
| `CS_AI_KNOWLEDGE_PROVIDER` | `dify` \| `local` \| `none` |
| `DIFY_BASE_URL` / `DIFY_API_KEY` / `DIFY_DATASET_ID` | Dify Dataset API |
| `CSAI_SECURITY_ENABLED` | Enable API key auth |
| `CSAI_API_KEY_CLIENT` / `CSAI_API_KEY_ADMIN` | Keys for client / admin |

Full reference: [docs/configuration.md](docs/configuration.md)  
Example env files: `.env.example`, `.env.local-llama.example`

---

## API examples

```bash
# Runtime (model source / knowledge provider)
curl -s http://localhost:8081/api/v1/runtime | jq

# Chat (security off)
curl -s -X POST http://localhost:8081/api/v1/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"How do I request a refund?"}' | jq

# Chat (security on)
curl -s -X POST http://localhost:8081/api/v1/chat \
  -H 'Content-Type: application/json' \
  -H "X-API-Key: $CSAI_API_KEY_CLIENT" \
  -d '{"message":"How do I request a refund?"}' | jq
```

Response highlights: `route` (intent/models/rag), `sources`, `degraded`, `handoff`.

---

## Security

For any shared or production environment:

```bash
export CSAI_SECURITY_ENABLED=true
export CSAI_API_KEY_CLIENT="$(openssl rand -hex 24)"
export CSAI_API_KEY_ADMIN="$(openssl rand -hex 24)"
```

- Report vulnerabilities privately — see [SECURITY.md](SECURITY.md)
- Operations guide — [docs/operations/RUNBOOK.md](docs/operations/RUNBOOK.md)

---

## Documentation

| Doc | Description |
|-----|-------------|
| [docs/README.md](docs/README.md) | Full documentation index |
| [docs/getting-started.md](docs/getting-started.md) | Setup guide |
| [docs/architecture.md](docs/architecture.md) | Architecture |
| [docs/configuration.md](docs/configuration.md) | Configuration |
| [docs/OPENSOURCE.md](docs/OPENSOURCE.md) | Publishing & community notes |
| [CHANGELOG.md](CHANGELOG.md) | Releases |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Contribution guide |

Deep-dive Chinese product/design docs live under `docs/requirements/`, `docs/superpowers/`, and `docs/development/`.

---

## Project layout

```text
customer-service-ai/
├── src/main/java/com/enterprise/csai/   # Application modules
├── scripts/                             # Install, smoke, local-llm helpers
├── docs/                                # Community & design documentation
├── samples/                             # Sample knowledge text
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

---

## Tests

```bash
mvn test
```

Unit tests do not require external LLMs. Integration tests that need Docker may be skipped if Docker is unavailable.

---

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) and follow the [Code of Conduct](CODE_OF_CONDUCT.md).

Maintainer: **andy yang**

---

## License

Copyright © 2026 **andy yang**

Licensed under the [Apache License, Version 2.0](LICENSE).
See also [NOTICE](NOTICE) for third-party attributions.
