# Configuration Reference

Author: **andy yang**

Configuration is driven by Spring Boot + `csai.*` properties. Prefer environment variables (see `.env.example`).

## Profiles

| Profile | Purpose |
|---------|---------|
| (default) | Local / real models |
| `mock` | Offline CI: deterministic models, no external LLM |
| `prod` | Forces security on; conservative timeouts |

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Core environment variables

### Database

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | Port |
| `DB_NAME` | `csai` | Database |
| `DB_USER` | `csai` | User |
| `DB_PASSWORD` | `csai` | Password |

### Model source

| Variable | Default | Description |
|----------|---------|-------------|
| `CS_AI_MODEL_SOURCE` | `local` | `local` or `cloud` |
| `CS_AI_DEFAULT_BASE_URL` | local llama URL | OpenAI-compatible base (`.../v1`) |
| `CS_AI_DEFAULT_API_KEY` | `sk-local` | API key for chat endpoints |
| `CS_AI_CLASSIFIER_MODEL` | `local-qwen` | Classifier model name |
| `CS_AI_ANSWER_STRONG_MODEL` | `local-qwen` | Strong answer model |
| `CS_AI_ANSWER_FAST_MODEL` | `local-qwen` | Fast answer model |
| `CS_AI_EMBEDDING_*` | local embed | Used when knowledge=`local` |

### Cloud (when `CS_AI_MODEL_SOURCE=cloud`)

| Variable | Description |
|----------|-------------|
| `CS_AI_CLOUD_BASE_URL` | e.g. Bailian `compatible-mode/v1` |
| `CS_AI_CLOUD_API_KEY` | Cloud API key |
| `CS_AI_CLOUD_*_MODEL` | Classifier / answer models |

### Knowledge (Dify)

| Variable | Description |
|----------|-------------|
| `CS_AI_KNOWLEDGE_PROVIDER` | `dify` / `local` / `none` |
| `DIFY_BASE_URL` | Dataset API prefix ending with `/v1` |
| `DIFY_API_KEY` | Dataset API key |
| `DIFY_DATASET_ID` | Dataset UUID |
| `DIFY_SEARCH_METHOD` | e.g. `semantic_search` |
| `DIFY_SEGMENT_FALLBACK` | `true` recommended on Dify RC |

### Security

| Variable | Default | Description |
|----------|---------|-------------|
| `CSAI_SECURITY_ENABLED` | `false` | Enable API key filter |
| `CSAI_API_KEY_CLIENT` | dev default | Client role key |
| `CSAI_API_KEY_ADMIN` | dev default | Admin + client roles |
| `CSAI_RATE_LIMIT_ENABLED` | `true` | Rate limit filter |
| `CSAI_RATE_LIMIT_RPM` | `120` | Requests per minute per principal |

### Resilience & routing

| Variable | Default | Description |
|----------|---------|-------------|
| `CSAI_CLASSIFIER_TIMEOUT_MS` | `90000` | Classifier timeout |
| `CSAI_ANSWER_TIMEOUT_MS` | `180000` | Answer timeout |
| `CSAI_DIFY_TIMEOUT_MS` | `8000` | Dify HTTP timeout |
| `CSAI_CONFIDENCE_THRESHOLD` | `0.55` | Below → handoff |
| `CSAI_HANDOFF_ON_UNKNOWN` | `true` | Handoff on UNKNOWN intent |
| `CSAI_REQUIRE_EVIDENCE` | `true` | Empty RAG → insufficient evidence reply |

## HTTP headers

```http
X-API-Key: <client-or-admin-key>
# or
Authorization: Bearer <client-or-admin-key>
```

## Important paths

| Path | Notes |
|------|-------|
| `GET /actuator/health` | Public |
| `GET /api/v1/ping` | Public |
| `POST /api/v1/chat` | Auth when security enabled |
| `POST /api/v1/chat/stream` | SSE |
| `GET /api/v1/runtime` | Active model/knowledge view |
| `/admin/**` | Requires `ADMIN` role when security enabled |

## Secrets hygiene

- Never commit `.env`
- Rotate keys before production
- Prefer a secret manager in cloud deployments
