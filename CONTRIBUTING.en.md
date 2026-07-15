# Contributing

Thank you for your interest in **customer-service-ai**.

Maintainer: **andy yang**

> 简体中文：[CONTRIBUTING.md](CONTRIBUTING.md)

## Ways to contribute

- Report bugs and propose features via GitHub Issues  
- Improve documentation and samples  
- Submit focused pull requests  
- Share deployment notes for different backends  

## Development setup

```bash
./scripts/install-framework.sh   # first time
docker compose up -d postgres
cp .env.example .env
mvn test
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

See [docs/getting-started.md](docs/getting-started.md) (Chinese).

## Guidelines

- Keep Spring Boot version aligned with the framework parent  
- Prefer ports/adapters for external systems  
- Never commit secrets  
- Add/update tests for behavior changes  
- Prefer backward-compatible `/api/v1` changes  

## Pull requests

1. Fork from latest `main` and open a focused branch  
2. Run locally:

   ```bash
   ./scripts/install-framework.sh
   mvn test
   bash ./scripts/ci-secret-scan.sh
   ```

3. Open a PR with the template filled in  
4. Wait for CI jobs `test` and `secrets-hygiene` to pass  
5. Maintainer merges (squash preferred)

### Maintainer note (solo)

| Case | Rule |
|------|------|
| **Small daily changes** | Maintainer may **push `main` directly** (no PR) |
| **PR merge** | **All CI must be green** (`test` + `secrets-hygiene`) before merge |
| External contributions | PR only |

Direct pushes still trigger Actions—if CI goes red, push a fix promptly. When merging Dependabot/community PRs, wait for both checks; do not use “Merge without waiting…”.

## License

By contributing, you agree that your contributions are licensed under **Apache License 2.0**.
