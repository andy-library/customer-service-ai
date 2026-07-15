#!/usr/bin/env bash
# Production-oriented smoke: auth, chat, rate-limit optional
set -euo pipefail
export PATH="/usr/bin:/bin:/usr/sbin:/sbin:/opt/homebrew/bin:${PATH:-}"

BASE="${1:-http://127.0.0.1:8081}"
KEY="${CSAI_API_KEY_CLIENT:-csai-client-dev-key}"
SEC="${CSAI_SECURITY_ENABLED:-false}"

pass=0
fail=0
ok() { echo "PASS  $1"; pass=$((pass+1)); }
bad() { echo "FAIL  $1"; fail=$((fail+1)); }

echo "== health =="
if curl -sf "$BASE/actuator/health" >/dev/null; then ok health; else bad health; fi

echo "== ping =="
if curl -sf "$BASE/api/v1/ping" >/dev/null; then ok ping; else bad ping; fi

if [[ "$SEC" == "true" ]]; then
  echo "== auth required =="
  code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/v1/chat" \
    -H 'Content-Type: application/json' -d '{"message":"hi"}' || true)
  if [[ "$code" == "401" ]]; then ok "chat_without_key_401"; else bad "chat_without_key got $code"; fi

  code=$(curl -s -o /tmp/csai-smoke-chat.json -w "%{http_code}" -X POST "$BASE/api/v1/chat" \
    -H "Content-Type: application/json" -H "X-API-Key: $KEY" \
    -d '{"message":"你好"}' --max-time 180 || true)
  if [[ "$code" == "200" ]]; then ok "chat_with_key"; else bad "chat_with_key $code"; head -c 300 /tmp/csai-smoke-chat.json; echo; fi
else
  echo "== chat (security off) =="
  code=$(curl -s -o /tmp/csai-smoke-chat.json -w "%{http_code}" -X POST "$BASE/api/v1/chat" \
    -H "Content-Type: application/json" \
    -d '{"message":"你好"}' --max-time 180 || true)
  if [[ "$code" == "200" ]]; then ok chat; else bad "chat $code"; fi
fi

if [[ -f /tmp/csai-smoke-chat.json ]]; then
  python3 - <<'PY' 2>/dev/null || true
import json
from pathlib import Path
p=Path("/tmp/csai-smoke-chat.json")
if p.exists():
  d=json.loads(p.read_text())
  data=d.get("data",d)
  print("intent=", (data.get("route") or {}).get("intent"))
  print("handoff=", data.get("handoff"))
  print("degraded=", data.get("degraded"))
  print("answer=", (data.get("answer") or "")[:200])
PY
fi

echo "Result pass=$pass fail=$fail"
[[ $fail -eq 0 ]]
