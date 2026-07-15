#!/usr/bin/env bash
# Start customer-service-ai with real OpenAI-compatible models (no mock profile).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

PORT="${SERVER_PORT:-8081}"
ENV_FILE="${ENV_FILE:-$ROOT/.env}"

"$ROOT/scripts/check-env.sh" "$ENV_FILE"

# shellcheck disable=SC1090
set -a
# shellcheck source=/dev/null
source "$ENV_FILE"
set +a

if [[ -S "${HOME}/.rd/docker.sock" && -z "${DOCKER_HOST:-}" ]]; then
  export DOCKER_HOST="unix://${HOME}/.rd/docker.sock"
fi

echo "Ensuring PostgreSQL (pgvector) is up..."
docker compose up -d postgres

echo "Waiting for Postgres..."
for i in $(seq 1 30); do
  if docker compose exec -T postgres pg_isready -U "${DB_USER:-csai}" -d "${DB_NAME:-csai}" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

echo "Starting app on port ${PORT} (profile=default, real models)..."
echo "Admin:  http://localhost:${PORT}/admin"
echo "Chat:   POST http://localhost:${PORT}/api/v1/chat"
exec mvn spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=${PORT}"
