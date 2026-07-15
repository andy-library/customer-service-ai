#!/usr/bin/env bash
# Smoke test against a running real-model instance. Does not print full secrets.
set -euo pipefail
BASE="${1:-http://localhost:8081}"

echo "== ping =="
curl -sf "$BASE/api/v1/ping" | head -c 400; echo

echo "== models =="
curl -sf "$BASE/api/v1/models" | head -c 600; echo

echo "== upload sample =="
curl -sf -X POST "$BASE/api/v1/knowledge/documents" \
  -F "file=@samples/refund-policy.md" \
  -F "title=退款政策" | head -c 500; echo

echo "== chat chitchat =="
curl -sf -X POST "$BASE/api/v1/chat" \
  -H 'Content-Type: application/json' \
  -d '{"message":"你好"}' | head -c 800; echo

echo "== chat billing/rag =="
curl -sf -X POST "$BASE/api/v1/chat" \
  -H 'Content-Type: application/json' \
  -d '{"message":"如何申请退款？"}' | head -c 1200; echo

echo "Smoke done. Check route.intent / sources in responses above."
