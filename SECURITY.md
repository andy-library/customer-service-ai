# Security Policy

## Supported versions

| Version       | Supported          |
|---------------|--------------------|
| 0.2.x-rc      | Yes (active)       |
| 0.1.x         | Limited (security fixes case-by-case) |
| < 0.1         | No                 |

## Reporting a vulnerability

Please **do not** create public GitHub issues for security vulnerabilities.

Prefer one of:

1. GitHub **Security Advisories** (private) on this repository when available
2. Email the maintainer: **andy yang** via the GitHub profile contact linked from the repository owner account

Include:

- Description and impact
- Reproduction steps / PoC (if available)
- Affected version / commit
- Suggested fix (optional)

You should receive an acknowledgement within **7 days**. We will coordinate a
fix and disclosure timeline with you.

## Security features in this project

- Optional API Key authentication (`CSAI_SECURITY_ENABLED=true`)
- Role separation for Admin vs Client
- Rate limiting
- Session ownership checks
- Input guardrails (basic injection patterns)
- Secret values should only live in environment variables / secret managers

## Hardening checklist for production

1. Enable `CSAI_SECURITY_ENABLED=true` and use strong random API keys
2. Do not expose Admin UI to the public internet without additional protection
3. Terminate TLS at a reverse proxy / ingress
4. Restrict network access to Dify, model endpoints, and PostgreSQL
5. Rotate keys regularly; never commit `.env`
6. Review rate limits and model timeouts for your capacity
