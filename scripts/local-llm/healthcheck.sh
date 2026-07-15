#!/usr/bin/env bash
set -euo pipefail

API_KEY="${API_KEY:-sk-local}"
CHAT="${CHAT_BASE:-http://127.0.0.1:8082/v1}"
EMBED="${EMBED_BASE:-http://127.0.0.1:8083/v1}"
CHAT_MODEL="${CHAT_MODEL:-qwen3.6-35b}"
EMBED_MODEL="${EMBED_MODEL:-qwen3-embedding}"

echo "== Chat models =="
curl -sS -H "Authorization: Bearer ${API_KEY}" "${CHAT}/models" | head -c 800; echo

echo "== Chat completions =="
curl -sS -H "Authorization: Bearer ${API_KEY}" -H "Content-Type: application/json" \
  "${CHAT}/chat/completions" \
  -d "{\"model\":\"${CHAT_MODEL}\",\"messages\":[{\"role\":\"user\",\"content\":\"你好，用一句话介绍你自己\"}],\"max_tokens\":64}" \
  | head -c 1000; echo

echo "== Embeddings =="
if curl -sS -o /tmp/csai-emb.json -w "%{http_code}" -H "Authorization: Bearer ${API_KEY}" -H "Content-Type: application/json" \
  "${EMBED}/embeddings" \
  -d "{\"model\":\"${EMBED_MODEL}\",\"input\":\"退款政策测试\"}" | grep -q 200; then
  python3 - <<'PY'
import json
from pathlib import Path
d=json.loads(Path("/tmp/csai-emb.json").read_text())
vec=d["data"][0]["embedding"]
print(f"OK embedding dim={len(vec)} model={d.get('model')}")
PY
else
  echo "FAIL embeddings (is start-embed.sh running? model downloaded?)"
  head -c 400 /tmp/csai-emb.json 2>/dev/null; echo
  exit 1
fi
