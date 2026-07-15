# Security Policy

Author: **andy yang**

> 简体中文：[SECURITY.md](SECURITY.md)

## Supported versions

| Version | Supported |
|---------|-----------|
| 0.2.x-rc | Yes |
| 0.1.x | Limited |
| < 0.1 | No |

## Reporting a vulnerability

Do **not** open public issues for security vulnerabilities.

Prefer GitHub Security Advisories (private) or contact the maintainer privately via the GitHub organization **andy-library**.

Include impact, reproduction steps, affected version, and optional fix suggestions.

## Production hardening

1. Enable `CSAI_SECURITY_ENABLED=true` with strong random keys  
2. Do not expose Admin UI to the public internet unprotected  
3. Terminate TLS at reverse proxy / ingress  
4. Restrict network access to Dify, model endpoints, and PostgreSQL  
5. Rotate keys; never commit `.env`  
