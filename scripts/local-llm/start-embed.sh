#!/usr/bin/env bash
# Start local Embedding model for RAG via llama-server --embedding.
set -euo pipefail

HOST="${HOST:-127.0.0.1}"
PORT="${PORT:-8083}"
CTX="${CTX:-8192}"
NGL="${NGL:-99}"
ALIAS="${ALIAS:-qwen3-embedding}"
API_KEY="${API_KEY:-sk-local}"

# Override after download, e.g.:
#   MODEL=/Users/andy.yang/LocalModels/Qwen3-Embedding-0.6B/xxx.gguf
#   ALIAS=qwen3-embedding
# or bge-m3:
#   MODEL=/Users/andy.yang/LocalModels/bge-m3/bge-m3-Q4_K_M.gguf ALIAS=bge-m3
MODEL="${MODEL:-}"
if [[ -z "$MODEL" ]]; then
  # auto-pick first gguf under common dirs
  for d in \
    /Users/andy.yang/LocalModels/Qwen3-Embedding-0.6B \
    /Users/andy.yang/LocalModels/Qwen3-Embedding \
    /Users/andy.yang/LocalModels/bge-m3
  do
    if [[ -d "$d" ]]; then
      cand="$(find "$d" -maxdepth 1 -type f -name '*.gguf' ! -iname 'mmproj*' | sort | head -n 1 || true)"
      if [[ -n "$cand" ]]; then
        MODEL="$cand"
        break
      fi
    fi
  done
fi

LLAMA_SERVER="${LLAMA_SERVER:-llama-server}"
LOG="${LOG:-/tmp/csai-llama-embed.log}"
PID_FILE="${PID_FILE:-/tmp/csai-llama-embed.pid}"

if [[ -z "${MODEL}" || ! -f "$MODEL" ]]; then
  cat >&2 <<'EOF'
ERROR: No embedding GGUF found.

Download one (recommended Qwen3-Embedding-0.6B), e.g.:

  mkdir -p /Users/andy.yang/LocalModels/Qwen3-Embedding-0.6B
  cd /Users/andy.yang/LocalModels/Qwen3-Embedding-0.6B
  huggingface-cli download Qwen/Qwen3-Embedding-0.6B-GGUF --include "*.gguf" --local-dir .

Then re-run: ./scripts/local-llm/start-embed.sh
Or: MODEL=/path/to/model.gguf ./scripts/local-llm/start-embed.sh
EOF
  exit 1
fi

if lsof -nP -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Port $PORT already in use."
  lsof -nP -iTCP:"$PORT" -sTCP:LISTEN || true
  exit 1
fi

echo "Starting Embedding llama-server"
echo "  model : $MODEL"
echo "  listen: http://${HOST}:${PORT}/v1"
echo "  alias : $ALIAS"
echo "  log   : $LOG"

nohup "$LLAMA_SERVER" \
  -m "$MODEL" \
  --host "$HOST" \
  --port "$PORT" \
  -c "$CTX" \
  -ngl "$NGL" \
  --alias "$ALIAS" \
  --api-key "$API_KEY" \
  --embedding \
  --pooling mean \
  --embd-normalize 2 \
  >"$LOG" 2>&1 &

echo $! >"$PID_FILE"
echo "PID $(cat "$PID_FILE")"
echo "Test: curl -s http://${HOST}:${PORT}/v1/embeddings -H \"Authorization: Bearer ${API_KEY}\" -H 'Content-Type: application/json' -d '{\"model\":\"${ALIAS}\",\"input\":\"测试\"}'"
