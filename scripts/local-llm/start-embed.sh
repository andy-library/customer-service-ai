#!/usr/bin/env bash
# Start local Embedding model via llama-server --embedding.
# Defaults: port 18081, alias local-bge-m3
# Note: with knowledge.provider=dify, Spring AI does not need local embed for RAG.
set -euo pipefail

HOST="${HOST:-127.0.0.1}"
PORT="${PORT:-18081}"
CTX="${CTX:-8192}"
NGL="${NGL:-99}"
ALIAS="${ALIAS:-local-bge-m3}"
API_KEY="${API_KEY:-sk-local}"

MODEL_DIR="${MODEL_DIR:-${HOME}/LocalModels}"
MODEL="${MODEL:-}"
if [[ -z "$MODEL" ]]; then
  for d in \
    "${MODEL_DIR}/bge-m3" \
    "${MODEL_DIR}/BAAI-bge-m3" \
    "${MODEL_DIR}/Qwen3-Embedding-0.6B" \
    "${MODEL_DIR}/Qwen3-Embedding" \
    "${MODEL_DIR}"
  do
    if [[ -d "$d" ]]; then
      cand="$(find "$d" -maxdepth 2 -type f -name '*.gguf' ! -iname 'mmproj*' 2>/dev/null | sort | head -n 1 || true)"
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

Download an embedding model (e.g. bge-m3, dim=1024), then:

  MODEL=/path/to/model.gguf ALIAS=local-bge-m3 ./scripts/local-llm/start-embed.sh
  # or: MODEL_DIR=$HOME/LocalModels ./scripts/local-llm/start-embed.sh
EOF
  exit 1
fi

if lsof -nP -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Port $PORT already in use (OK if embed llama is already running)."
  lsof -nP -iTCP:"$PORT" -sTCP:LISTEN || true
  exit 0
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
echo "Test: curl -s http://${HOST}:${PORT}/v1/embeddings -H \"Authorization: Bearer ${API_KEY}\" -H 'Content-Type: application/json' -d '{\"model\":\"${ALIAS}\",\"input\":\"test\"}'"
