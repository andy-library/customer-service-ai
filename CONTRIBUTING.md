# Contributing

Thank you for your interest in **customer-service-ai**.

Maintainer / author: **andy yang**

## Ways to contribute

- Report bugs and propose features via GitHub Issues
- Improve documentation and samples
- Submit pull requests for bug fixes and well-scoped features
- Share deployment notes for different model backends (llama.cpp, cloud providers, Dify)

## Development setup

1. **Prerequisites**
   - OpenJDK 21+
   - Maven 3.9+
   - Docker (PostgreSQL / optional full stack)
   - Optional: local llama.cpp and/or Dify for real-model tests

2. **Install the framework parent** (first time)

   ```bash
   ./scripts/install-framework.sh
   ```

3. **Run tests (offline mock path)**

   ```bash
   mvn test
   ```

4. **Run the app**

   ```bash
   docker compose up -d postgres
   cp .env.example .env
   # edit .env as needed
   set -a && source .env && set +a
   mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
   ```

See [docs/getting-started.md](docs/getting-started.md) for full options.

## Coding guidelines

- Keep Boot version aligned with `microservice-framework` (do not upgrade Spring Boot casually)
- Prefer ports/adapters for external systems (models, knowledge)
- Do not commit secrets (`.env`, API keys, tokens)
- Add or update unit tests for behavior changes
- Keep public API (`/api/v1/**`) backward compatible when possible; document breaking changes in `CHANGELOG.md`
- Author metadata in contributions should use the name **andy yang** only when acting as project maintainer commits; community contributors use their own GitHub identity

## Pull request process

1. Fork and create a feature branch
2. Make focused commits with clear messages
3. Ensure `mvn test` passes
4. Open a PR with:
   - What changed and why
   - How to test
   - Linked issue (if any)
5. Be responsive to review feedback

## Issue reports

Please include:

- Version / commit
- Runtime (local / docker / k8s)
- Model backend (local/cloud) and knowledge provider (dify/local/none)
- Steps to reproduce
- Logs (redact secrets)

## Security issues

Do **not** open public issues for vulnerabilities. See [SECURITY.md](SECURITY.md).

## License

By contributing, you agree that your contributions will be licensed under the **Apache License 2.0**.
