# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [0.2.0-rc.1] — 2026-07-15

### Added
- Production-oriented security: API Key auth (`X-API-Key` / Bearer), admin role checks
- Rate limiting (sliding window) with HTTP 429
- Session ownership (`owner_id`) and cross-principal protection
- Model / Dify timeouts and bounded stream thread pool
- Guardrails (system prompt, evidence policy, basic injection patterns)
- Human handoff placeholder (`handoff` / `handoffReason`)
- Knowledge search degradation signals (`degraded` / `degradedReasons`)
- Audit log table and Micrometer business metrics
- Health indicator details for model/knowledge/security
- Docker packaging (`Dockerfile`, compose `full` profile)
- Operations runbook and production smoke script
- Schema ensure safety net for legacy databases

### Changed
- Default knowledge provider: Dify Dataset API
- Pluggable model source: `local` (llama.cpp) / `cloud` (OpenAI-compatible)
- Version bump to quasi-production release candidate

### Known limitations
- Dify 1.16.x RC may fail Dataset `/retrieve` after hits; segment fallback is enabled by default
- Admin UI expects API key headers (browser needs extension or reverse proxy)
- Full multi-tenant SaaS, ticketing, and multi-channel connectors are out of scope

## [0.1.0] — 2026-07-15

### Added
- MVP modular monolith on microservice-framework + Spring AI
- Intent routing, multi-model gateway, chat orchestration (sync + SSE)
- Optional local PgVector ingest path
- Admin Thymeleaf console
- Mock profile for offline tests

[0.2.0-rc.1]: https://github.com/andy-yang/customer-service-ai/releases/tag/v0.2.0-rc.1
[0.1.0]: https://github.com/andy-yang/customer-service-ai/releases/tag/v0.1.0
