# Implementation progress

Maintainer: **andy yang**  
Branch (historical): `feat/csai-mvp`  
Version: **0.2.0-rc.1**

## Status

Quasi-production release candidate is ready for open-source publication.

## Delivered

- Modular monolith on microservice-framework + Spring AI
- Intent routing, multi-model gateway, chat orchestration
- Dify knowledge (primary) + optional local PgVector
- Local / cloud model source switch
- API Key security, rate limit, session ownership
- Guardrails, handoff placeholder, audit, metrics
- Docker packaging + RUNBOOK + smoke scripts
- Open-source documentation package (LICENSE, CONTRIBUTING, docs/*)

## Known limitations

- Dify 1.16.x RC may fail Dataset `/retrieve` after hits → segment fallback
- Admin UI requires API key headers when security is enabled
- Framework parent may need private install for some environments

## Docs

See [docs/README.md](../../README.md) and repository root [README.md](../../../README.md).
