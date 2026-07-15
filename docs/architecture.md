# Architecture Overview

Author: **andy yang**

## Purpose

**customer-service-ai** is a **modular monolith** that orchestrates enterprise customer-service conversations:

1. Authenticate and rate-limit callers  
2. Classify user intent with an LLM  
3. Select an answer model by policy  
4. Retrieve knowledge (Dify by default)  
5. Generate grounded answers with sources  
6. Persist session, route logs, and audit events  
7. Expose metrics and health for operations  

It is **not** a full ticketing system or multi-channel hub. It is the **AI orchestration layer**.

## High-level diagram

```
                 ┌──────────────────────────────┐
                 │  Clients / BFF / API Gateway │
                 └──────────────┬───────────────┘
                                │  X-API-Key / Bearer
                 ┌──────────────▼───────────────┐
                 │     customer-service-ai      │
                 │  REST + SSE + Admin (Thymeleaf)
                 │                              │
                 │  security · rate limit       │
                 │  router · guardrail · handoff│
                 │  chat orchestrator           │
                 │  metrics · audit · sessions  │
                 └───┬───────────────────┬──────┘
                     │                   │
          OpenAI-compatible LLM    Knowledge provider
          (local llama / cloud)    (Dify / local PgVector / none)
                     │                   │
                  PostgreSQL ◄───────────┘
                  (sessions, logs, audit)
```

## Modules (package layout)

```
com.enterprise.csai
├── admin            # Thymeleaf ops UI
├── audit            # Audit persistence
├── chat             # Orchestration, sessions, messages
├── common           # Config, errors, schema helpers
├── domain
│   ├── policy       # Guardrail, handoff
│   └── port         # KnowledgePort, ModelPort
├── knowledge        # Dify / local / none retrievers
├── modelgateway     # Multi-model registry & OpenAI clients
├── observability    # Metrics & health
├── router           # Intent classification
└── security         # API key & rate limit filters
```

## Request lifecycle (chat)

```
POST /api/v1/chat            (sync)
POST /api/v1/chat/stream     (SSE: status / delta / meta)
  → ApiKeyAuthFilter (optional)
  → RateLimitFilter
  → ChatOrchestrator
       → GuardrailPolicy (injection / evidence)
       → RoutingService (classifier model)
       → HandoffPolicy (optional short-circuit)
       → KnowledgePort.search (if RAG needed)
       → ModelPort.chat / stream (answer model)
       → Persist session/messages/route/audit
       → Return answer + route + sources + degraded/handoff
```

Admin UI (`/admin/chat`) uses the streaming endpoint for token-by-token display.

## Pluggable backends

| Concern | Config key | Options |
|---------|------------|---------|
| Model source | `csai.model-source` | `local`, `cloud` |
| Knowledge | `csai.knowledge.provider` | `dify`, `local`, `none` |
| Security | `csai.security.enabled` | `true` / `false` |

Switching model source reloads the registry at process start (restart required).

## Resilience & degradation

- Timeouts for classifier, answer, and Dify HTTP calls  
- Dify retrieve failures can fall back to segment listing + keyword score  
- Responses expose `degraded` and `degradedReasons`  
- Classifier outage degrades to `UNKNOWN` without forcing handoff  

## Data stores

| Store | Usage |
|-------|--------|
| PostgreSQL | Sessions, messages, route logs, audit, optional local doc metadata |
| PgVector | Only when `knowledge.provider=local` |
| Dify | Primary enterprise knowledge index |

## Related docs

- Product requirements: [requirements/PRD.md](requirements/PRD.md)
- Configuration: [configuration.md](configuration.md)
- Operations: [operations/RUNBOOK.md](operations/RUNBOOK.md)
- Dify & models: [development/DIFY-AND-MODELS.md](development/DIFY-AND-MODELS.md)
