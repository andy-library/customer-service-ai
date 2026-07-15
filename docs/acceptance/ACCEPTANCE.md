# Acceptance Checklist

| Field | Value |
|-------|-------|
| App version | `0.2.0-rc.1` |
| PRD | [requirements/PRD.md](../requirements/PRD.md) |
| Author | andy yang |

Use this list for release or deployment verification.

## Security

- [ ] With `CSAI_SECURITY_ENABLED=true`, request without key → **401**
- [ ] Wrong key → **401**
- [ ] Valid client key → chat **200**
- [ ] Client key cannot access `/admin` → **403**
- [ ] Admin key can access `/admin` → **200**
- [ ] Session created by principal A cannot be continued by B → forbidden

## Chat & streaming

- [ ] `POST /api/v1/chat` returns `route` + `answer`
- [ ] `POST /api/v1/chat/stream` emits `status` / `delta` / `meta`
- [ ] Admin **对话测试** page streams tokens in the browser
- [ ] RAG questions return `sources` when knowledge hits
- [ ] `degraded` / `handoff` flags appear when policies trigger

## Knowledge & models

- [ ] `csai.knowledge.provider=dify` retrieves (or degrades with fallback)
- [ ] `provider=none` (mock) does not retrieve
- [ ] `CS_AI_MODEL_SOURCE=local` registers local-qwen (or configured alias)
- [ ] Switching to `cloud` uses `csai.cloud.*` after restart

## Ops

- [ ] `GET /actuator/health` → UP (with DB available)
- [ ] `GET /api/v1/runtime` shows modelSource + knowledgeProvider
- [ ] `mvn test` green
- [ ] `docs/operations/RUNBOOK.md` matches deployment steps
- [ ] No secrets committed (`.env` gitignored; examples empty)

## Smoke commands

```bash
curl -s http://127.0.0.1:8081/actuator/health
curl -s http://127.0.0.1:8081/api/v1/ping
curl -s -X POST http://127.0.0.1:8081/api/v1/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"hello"}'
# with security:
curl -s -X POST http://127.0.0.1:8081/api/v1/chat \
  -H 'Content-Type: application/json' \
  -H "X-API-Key: $CSAI_API_KEY_CLIENT" \
  -d '{"message":"hello"}'
```
