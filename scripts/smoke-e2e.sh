#!/usr/bin/env bash
# End-to-end smoke: llama + Dify retrieve + Spring AI chat
set -euo pipefail
export PATH="/usr/bin:/bin:/usr/sbin:/sbin:/opt/homebrew/bin:${PATH:-}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BASE="${1:-http://127.0.0.1:8081}"
ENV_FILE="${ENV_FILE:-$ROOT/.env}"

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

pass=0
fail=0
check() {
  local name="$1"
  shift
  if "$@"; then
    echo "PASS  $name"
    pass=$((pass + 1))
  else
    echo "FAIL  $name"
    fail=$((fail + 1))
  fi
}

echo "== 1) Local llama chat =="
check "chat models" curl -sf -o /dev/null "${CS_AI_DEFAULT_BASE_URL%/}/models"

echo "== 2) Dify API reachability =="
DIFY="${DIFY_BASE_URL:-http://127.0.0.1:15001/v1}"
check "dify health" curl -sf -o /dev/null "${DIFY%/v1}/health" || curl -sf -o /dev/null "http://127.0.0.1:15001/health"

if [[ -n "${DIFY_API_KEY:-}" && -n "${DIFY_DATASET_ID:-}" ]]; then
  echo "== 3) Dify retrieve =="
  code=$(curl -sS -o /tmp/csai-dify-retrieve.json -w "%{http_code}" \
    -H "Authorization: Bearer ${DIFY_API_KEY}" \
    -H "Content-Type: application/json" \
    -d '{"query":"退款","retrieval_model":{"search_method":"hybrid_search","reranking_enable":false,"top_k":3,"score_threshold_enabled":false}}' \
    "${DIFY}/datasets/${DIFY_DATASET_ID}/retrieve" || echo 000)
  if [[ "$code" == "200" ]]; then
    echo "PASS  dify retrieve HTTP 200"
    pass=$((pass + 1))
    head -c 400 /tmp/csai-dify-retrieve.json; echo
  else
    echo "FAIL  dify retrieve HTTP $code"
    head -c 400 /tmp/csai-dify-retrieve.json; echo
    fail=$((fail + 1))
  fi
else
  echo "SKIP  dify retrieve (DIFY_API_KEY / DIFY_DATASET_ID empty)"
fi

echo "== 4) Spring AI =="
check "ping" curl -sf -o /tmp/csai-ping.json "${BASE}/api/v1/ping"
check "runtime" curl -sf -o /tmp/csai-runtime.json "${BASE}/api/v1/runtime"
echo "runtime:"; python3 -m json.tool </tmp/csai-runtime.json 2>/dev/null | head -40 || cat /tmp/csai-runtime.json

check "knowledge status" curl -sf -o /tmp/csai-kstatus.json "${BASE}/api/v1/knowledge/status"
echo "knowledge:"; python3 -m json.tool </tmp/csai-kstatus.json 2>/dev/null || cat /tmp/csai-kstatus.json

echo "== 5) knowledge search =="
code=$(curl -sS -o /tmp/csai-ksearch.json -w "%{http_code}" \
  -H "Content-Type: application/json" \
  -d '{"query":"退款政策","topK":3}' \
  "${BASE}/api/v1/knowledge/search" || echo 000)
if [[ "$code" == "200" ]]; then
  echo "PASS  knowledge search HTTP 200"
  pass=$((pass + 1))
  python3 -m json.tool </tmp/csai-ksearch.json 2>/dev/null | head -50 || head -c 500 /tmp/csai-ksearch.json
else
  echo "FAIL  knowledge search HTTP $code"
  head -c 400 /tmp/csai-ksearch.json; echo
  fail=$((fail + 1))
fi

echo "== 6) chat =="
code=$(curl -sS -o /tmp/csai-chat.json -w "%{http_code}" \
  -H "Content-Type: application/json" \
  -d '{"message":"如何申请退款？请根据知识库简要回答。"}' \
  "${BASE}/api/v1/chat" || echo 000)
if [[ "$code" == "200" ]]; then
  echo "PASS  chat HTTP 200"
  pass=$((pass + 1))
  python3 - <<'PY'
import json
from pathlib import Path
d=json.loads(Path("/tmp/csai-chat.json").read_text())
data=d.get("data", d)
ans=(data.get("answer") or "")[:500]
route=data.get("routing") or data.get("route") or {}
sources=data.get("sources") or []
print("answer:", ans)
print("routing:", route)
print("sources:", len(sources))
for s in sources[:3]:
    print(" -", (s.get("title") or "")[:40], "score=", s.get("score"))
PY
else
  echo "FAIL  chat HTTP $code"
  head -c 600 /tmp/csai-chat.json; echo
  fail=$((fail + 1))
fi

echo
echo "Result: pass=$pass fail=$fail"
[[ $fail -eq 0 ]]
