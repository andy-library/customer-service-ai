# Documentation Index

**customer-service-ai** — enterprise intelligent customer service orchestration on Spring AI.

Author / maintainer: **andy yang**

## Start here

| Document | Audience | Description |
|----------|----------|-------------|
| [../README.md](../README.md) | Everyone | Project overview & quick start |
| [getting-started.md](getting-started.md) | Developers | Install, configure, run |
| [development/IDEA-启动配置.md](development/IDEA-启动配置.md) | Developers | IntelliJ IDEA run configurations |
| [architecture.md](architecture.md) | Developers / architects | System design overview |
| [configuration.md](configuration.md) | Operators | Environment variables & profiles |
| [operations/RUNBOOK.md](operations/RUNBOOK.md) | SRE / ops | Run, health, incidents |
| [../CONTRIBUTING.md](../CONTRIBUTING.md) | Contributors | How to contribute |
| [../SECURITY.md](../SECURITY.md) | Everyone | Vulnerability reporting |
| [../CHANGELOG.md](../CHANGELOG.md) | Everyone | Release history |

## Product & design (deep dive)

| Document | Description |
|----------|-------------|
| [requirements/PRD-智能客服-MVP.md](requirements/PRD-智能客服-MVP.md) | MVP product requirements |
| [requirements/PRD-智能客服-Production-v1.md](requirements/PRD-智能客服-Production-v1.md) | Production (P0+P1) requirements |
| [superpowers/specs/2026-07-15-intelligent-customer-service-design.md](superpowers/specs/2026-07-15-intelligent-customer-service-design.md) | MVP design spec |
| [superpowers/specs/2026-07-15-csai-production-refactor-design.md](superpowers/specs/2026-07-15-csai-production-refactor-design.md) | Production refactor design |
| [analysis/ARCHITECTURE-生产级评估-v1.md](analysis/ARCHITECTURE-生产级评估-v1.md) | Production readiness assessment |
| [acceptance/ACCEPTANCE-智能客服-Production-v1.md](acceptance/ACCEPTANCE-智能客服-Production-v1.md) | Acceptance checklist |

## Integration guides

| Document | Description |
|----------|-------------|
| [development/DIFY-知识库与可插拔模型.md](development/DIFY-知识库与可插拔模型.md) | Dify knowledge + local/cloud models |
| [development/LOCAL-LLAMA-全量部署-M1.md](development/LOCAL-LLAMA-全量部署-M1.md) | Local llama.cpp deployment notes |
| [development/BAILIAN-GLM-配置.md](development/BAILIAN-GLM-配置.md) | Alibaba Bailian / glm OpenAI-compatible setup |
| [development/DEV-智能客服-MVP.md](development/DEV-智能客服-MVP.md) | Development notes |
| [development/REAL-MODEL-联调.md](development/REAL-MODEL-联调.md) | Real model integration |

## Mental model

```
Client / Channel
      │
      ▼
customer-service-ai (this repo)
  • Auth / rate limit
  • Intent routing
  • Model gateway (local or cloud)
  • Knowledge retrieve (Dify primary)
  • Session / audit / metrics
      │
      ├──► OpenAI-compatible LLM endpoints
      └──► Dify Dataset API
```

## Language

Primary maintainer documentation is bilingual where helpful (English structure + Chinese deep-dive specs).
Code identifiers and API paths are English.
