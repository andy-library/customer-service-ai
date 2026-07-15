#!/usr/bin/env bash
set -euo pipefail

API_KEY="${API_KEY:-sk-local}"
# Align with running llama.cpp defaults used by this project (chat :18080, embed :18081)
CHAT="${CHAT_BASE:-http://127.0.0.1:18080/v1}"
EMBED="${EMBED_BASE:-http://127.0.0.1:18081/v1}"
CHAT_MODEL="${CHAT_MODEL:-local-qwen}"
EMBED_MODEL="${EMBED_MODEL:-local-bge-m3}"

echo "== Chat models (${CHAT}) =="
curl -sS -H "Authorization: Bearer ${API_KEY}" "${CHAT}/models" | head -c 800; echo

echo "== Chat completions (${CHAT_MODEL}) =="
curl -sS -H "Authorization: Bearer ${API_KEY}" -H "Content-Type: application/json" \
  "${CHAT}/chat/completions" \
  -d "{\"model\":\"${CHAT_MODEL}\",\"messages\":[{\"role\":\"user\",\"content\":\"你好，用一句话介绍你自己\"}],\"max_tokens\":64}" \
  | head -c 1000; echo

echo "== Embeddings (${EMBED} / ${EMBED_MODEL}) =="
if curl -sS -o /tmp/csai-emb.json -w "%{http_code}" -H "Authorization: Bearer ${API_KEY}" -H "Content-Type: application/json" \
  "${EMBED}/embeddings" \
  -d "{\"model\":\"${EMBED_MODEL}\",\"input\":\"退款政策测试\"}" | grep -q 200; then
  python3 - <<'PY'
import json
from pathlib import Path
d=json.loads(Path("/tmp/csai-emb.json").read_text())
vec=d["data"][0]["embedding"]
print(f"OK embedding dim={len(vec)} model={d.get('model')}")
if len(vec) != 1024:
    print(f"WARN: expected dim 1024 for bge-m3, got {len(vec)}")
PY
else
  echo "FAIL embeddings (is embed llama-server on :18081 running?)"
  head -c 400 /tmp/csai-emb.json 2>/dev/null; echo
  exit 1
fi

echo "All local llama checks passed."
