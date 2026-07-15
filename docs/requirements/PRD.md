# Product Requirements — customer-service-ai

| Field | Value |
|-------|-------|
| Version | **1.0** (matches app `0.2.0-rc.1`) |
| Status | **Implemented** |
| Author | andy yang |
| Scope | Quasi-production intelligent customer-service orchestration |

---

## 1. Goals

Provide a Spring AI **orchestration service** that:

- Classifies user intent and routes to appropriate models  
- Retrieves enterprise knowledge (Dify primary; optional local PgVector)  
- Generates grounded answers with **sources**  
- Supports **local** (llama.cpp) and **cloud** (OpenAI-compatible) model backends  
- Offers optional API-key security, rate limiting, timeouts, degradation signals, and handoff placeholders  
- Exposes health, metrics, audit, and Docker packaging  

### 1.1 Success criteria

| ID | Criterion |
|----|-----------|
| G1 | Unauthenticated access denied when security enabled (401) |
| G2 | Session ownership enforced across principals |
| G3 | Model/Dify timeouts and degraded knowledge paths |
| G4 | Rate limit returns 429 when exceeded |
| G5 | Responses include `route`, `sources`, optional `degraded` / `handoff` |
| G6 | Knowledge providers: `dify` \| `local` \| `none` |
| G7 | Handoff placeholder via policy (low confidence / user request / unknown) |
| G8 | Docker + runbook + smoke scripts available |
| G9 | Automated unit tests pass (`mvn test`) |

### 1.2 Non-goals

- Full ticketing / CRM  
- Multi-channel gateways (WeCom, DingTalk, …)  
- Multi-tenant SaaS billing  
- Microservice mesh split  
- Replacing Dify as the knowledge authoring platform  

---

## 2. Actors

| Actor | Needs |
|-------|--------|
| End user (via BFF/client) | Ask questions; get grounded answers |
| Operator | Admin UI for chat test, knowledge status, model list |
| Developer / SRE | Configure models, Dify, security; observe health/metrics |

---

## 3. Functional requirements (implemented)

### Chat

- `POST /api/v1/chat` — synchronous answer  
- `POST /api/v1/chat/stream` — SSE streaming (`status` / `delta` / `meta`)  
- Session continuity via `sessionId` + server-side history  
- Response fields: `answer`, `route`, `sources`, `degraded`, `degradedReasons`, `handoff`, `handoffReason`  

### Knowledge

- Provider `dify`: Dataset retrieve API + segment fallback when retrieve fails  
- Provider `local`: PgVector ingest/search  
- Provider `none`: no retrieval (e.g. mock profile)  

### Models

- `csai.model-source=local|cloud`  
- Roles: classifier, answer-strong, answer-fast  
- Timeouts for classifier / answer / Dify HTTP  

### Security & resilience

- Optional API keys (`X-API-Key` / Bearer), admin role for `/admin/**`  
- Rate limiting (sliding window)  
- Guardrails: system prompt, evidence policy, basic injection patterns  
- Handoff policy  

### Ops

- Flyway + schema ensure for ownership/audit columns  
- Micrometer metrics (`csai.*`)  
- Health indicator details  
- Admin Thymeleaf UI (streaming chat console)  
- Dockerfile, compose, smoke scripts  

---

## 4. API summary

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/v1/ping` | Public |
| GET | `/api/v1/runtime` | Active model/knowledge view |
| POST | `/api/v1/chat` | Sync |
| POST | `/api/v1/chat/stream` | SSE |
| POST | `/api/v1/knowledge/search` | Retrieve snippets |
| GET | `/admin/**` | Ops UI (ADMIN when security on) |
| GET | `/actuator/health` | Health |

JSON business APIs are wrapped by framework `ApiResponse` (`code` / `message` / `data`). SSE is not wrapped.

---

## 5. Configuration (see also configuration.md)

Key switches:

- `CS_AI_MODEL_SOURCE` — `local` \| `cloud`  
- `CS_AI_KNOWLEDGE_PROVIDER` — `dify` \| `local` \| `none`  
- `CSAI_SECURITY_ENABLED` — API key enforcement  
- `DIFY_BASE_URL` / `DIFY_API_KEY` / `DIFY_DATASET_ID`  

---

## 6. Acceptance

Use [../acceptance/ACCEPTANCE.md](../acceptance/ACCEPTANCE.md).
