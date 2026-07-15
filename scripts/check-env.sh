#!/usr/bin/env bash
# Validate .env for real-model 联调 without printing secret values.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${1:-$ROOT/.env}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "FAIL: $ENV_FILE not found"
  echo "  cp .env.example .env   # then fill CS_AI_* keys"
  exit 1
fi

# shellcheck disable=SC1090
set -a
# shellcheck source=/dev/null
source "$ENV_FILE"
set +a

ok=0
fail=0
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

echo "Checking $ENV_FILE"
check CS_AI_DEFAULT_BASE_URL
check CS_AI_DEFAULT_API_KEY
# optional overrides — if empty, defaults are used (OK when DEFAULT is set)
for n in CS_AI_CLASSIFIER_MODEL CS_AI_ANSWER_STRONG_MODEL CS_AI_ANSWER_FAST_MODEL CS_AI_EMBEDDING_MODEL; do
  if [[ -n "${!n-}" ]]; then
    echo "OK    $n=${!n}"
  else
    echo "INFO  $n not set (will use application.yml default / DEFAULT)"
  fi
done

if [[ $fail -gt 0 ]]; then
  echo "Result: FAIL ($fail missing required vars). Fill them before run-real.sh"
  exit 1
fi
echo "Result: PASS ($ok required vars set)"
echo "Note: providers must be OpenAI-compatible (OpenAI / DeepSeek / 通义 compatible mode / 智谱 OpenAI 协议等)."
echo "      Anthropic Claude native API is NOT supported by this MVP (use OpenAI-compatible gateway)."
