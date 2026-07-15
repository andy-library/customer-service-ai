#!/usr/bin/env bash
# Validate .env for local/cloud + Dify without printing secret values.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${1:-$ROOT/.env}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "FAIL: $ENV_FILE not found"
  echo "  cp .env.local-llama.example .env   # local llama + Dify"
  echo "  # or: cp .env.example .env"
  exit 1
fi

# shellcheck disable=SC1090
set -a
# shellcheck source=/dev/null
source "$ENV_FILE"
set +a

ok=0
fail=0
warn=0

check() {
  local name="$1"
  local val="${!name-}"
  if [[ -z "${val}" || "${val}" == "replace-me" || "${val}" == "sk-placeholder" ]]; then
    echo "MISS  $name"
    fail=$((fail + 1))
  else
    echo "OK    $name (${#val} chars)"
    ok=$((ok + 1))
  fi
}

info() {
  echo "INFO  $*"
}

echo "Checking $ENV_FILE"
SOURCE="${CS_AI_MODEL_SOURCE:-local}"
KNOWLEDGE="${CS_AI_KNOWLEDGE_PROVIDER:-dify}"
info "CS_AI_MODEL_SOURCE=${SOURCE}"
info "CS_AI_KNOWLEDGE_PROVIDER=${KNOWLEDGE}"

if [[ "${SOURCE}" == "cloud" ]]; then
  check CS_AI_CLOUD_BASE_URL
  check CS_AI_CLOUD_API_KEY
else
  check CS_AI_DEFAULT_BASE_URL
  check CS_AI_DEFAULT_API_KEY
fi

for n in CS_AI_CLASSIFIER_MODEL CS_AI_ANSWER_STRONG_MODEL CS_AI_ANSWER_FAST_MODEL; do
  if [[ -n "${!n-}" ]]; then
    echo "OK    $n=${!n}"
  else
    info "$n not set (will use application.yml defaults)"
  fi
done

if [[ "${KNOWLEDGE}" == "dify" ]]; then
  check DIFY_BASE_URL
  if [[ -z "${DIFY_API_KEY-}" ]]; then
    echo "WARN  DIFY_API_KEY empty — retrieval returns empty hits until configured"
    warn=$((warn + 1))
  else
    echo "OK    DIFY_API_KEY (${#DIFY_API_KEY} chars)"
    ok=$((ok + 1))
  fi
  if [[ -z "${DIFY_DATASET_ID-}" ]]; then
    echo "WARN  DIFY_DATASET_ID empty — retrieval returns empty hits until configured"
    warn=$((warn + 1))
  else
    echo "OK    DIFY_DATASET_ID (${#DIFY_DATASET_ID} chars)"
    ok=$((ok + 1))
  fi
elif [[ "${KNOWLEDGE}" == "local" ]]; then
  check CS_AI_EMBEDDING_BASE_URL
  check CS_AI_EMBEDDING_MODEL
fi

if [[ $fail -gt 0 ]]; then
  echo "Result: FAIL ($fail missing required vars)"
  exit 1
fi
echo "Result: PASS ($ok required vars set, $warn warnings)"
echo "Switch local/cloud: CS_AI_MODEL_SOURCE=local|cloud then restart the app."
echo "Knowledge: manage docs in Dify when provider=dify; Spring only calls retrieve API."
