# Open Source Guide

This repository is prepared for public GitHub hosting.

**Author / Copyright holder:** andy yang  
**License:** Apache License 2.0 (see [LICENSE](../LICENSE) and [NOTICE](../NOTICE))

## What is published

- Application source under `src/`
- Scripts under `scripts/` (no secrets)
- Documentation under `docs/`
- Sample knowledge file under `samples/`
- Docker / Compose packaging
- Community files: CONTRIBUTING, CODE_OF_CONDUCT, SECURITY, CHANGELOG

## What must never be published

- `.env` (gitignored)
- Real API keys, Dataset tokens, cloud credentials
- Model weight files (`*.gguf`)
- Private infrastructure hostnames/passwords

## Maintainer identity

All project authorship for this open-source release is attributed to:

```text
andy yang
```

Do not use Chinese legal names or private machine hostnames in commits or docs.

Recommended local Git config for this repository:

```bash
git config user.name "andy yang"
git config user.email "YOUR_PUBLIC_GITHUB_NOREPLY@users.noreply.github.com"
```

If historical commits contain other author names, rewrite history **before the first public push**:

```bash
git filter-branch -f --env-filter '
export GIT_AUTHOR_NAME="andy yang"
export GIT_AUTHOR_EMAIL="YOUR_PUBLIC_GITHUB_NOREPLY@users.noreply.github.com"
export GIT_COMMITTER_NAME="andy yang"
export GIT_COMMITTER_EMAIL="YOUR_PUBLIC_GITHUB_NOREPLY@users.noreply.github.com"
' -- --all
```

(Or use `git filter-repo` if available.)

## Suggested GitHub repository settings

1. Create repository (example name: `customer-service-ai`)
2. Enable Issues + Discussions (optional)
3. Enable private vulnerability reporting
4. Protect `main` with required CI checks
5. Add topics: `spring-ai`, `java`, `dify`, `rag`, `customer-service`, `openai-compatible`
6. Set description:

   > Enterprise intelligent customer service orchestration with Spring AI, pluggable local/cloud LLMs, and Dify knowledge retrieval.

## First publish checklist

- [ ] `git status` clean; no `.env`
- [ ] Author name is **andy yang** for release commits
- [ ] `README.md` clone URL matches your GitHub path
- [ ] `pom.xml` `<url>` / `<scm>` match your GitHub path
- [ ] CI green or documented as “requires private parent install”
- [ ] Tag `v0.2.0-rc.1` after push

```bash
git remote add origin git@github.com:YOUR_ORG/customer-service-ai.git
git push -u origin main
git tag -a v0.2.0-rc.1 -m "v0.2.0-rc.1"
git push origin v0.2.0-rc.1
```

## Community reading path

1. [README.md](../README.md)  
2. [getting-started.md](getting-started.md)  
3. [architecture.md](architecture.md)  
4. [configuration.md](configuration.md)  
5. [operations/RUNBOOK.md](operations/RUNBOOK.md)  
6. Production design under `docs/superpowers/specs/`  

## Dependency note

Runtime parent `microservice-framework-starter-parent` may be private to your org.
Open-source consumers must either:

- Install it from your published Maven coordinates, or  
- Use `scripts/install-framework.sh` if the source is available, or  
- Fork and adapt the parent to public Spring Boot BOM only (contribution welcome).

Document your chosen approach in the repository description when publishing.
