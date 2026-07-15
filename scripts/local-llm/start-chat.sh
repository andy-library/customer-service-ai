#!/usr/bin/env bash
# Start local Chat model via llama-server (OpenAI-compatible).
# Defaults: port 18080, alias local-qwen
set -euo pipefail

HOST="${HOST:-127.0.0.1}"
PORT="${PORT:-18080}"
CTX="${CTX:-32768}"
NGL="${NGL:-99}"
ALIAS="${ALIAS:-local-qwen}"
API_KEY="${API_KEY:-sk-local}"

# Prefer MODEL env; otherwise look under MODEL_DIR (default: $HOME/LocalModels)
MODEL_DIR="${MODEL_DIR:-${HOME}/LocalModels}"
MODEL="${MODEL:-}"
if [[ -z "$MODEL" ]]; then
  for cand in \
    "${MODEL_DIR}/Qwen3.6-35B-A3B"/*.gguf \
    "${MODEL_DIR}"/**/*.gguf
  do
    if [[ -f "$cand" ]]; then
      MODEL="$cand"
      break
    fi
  done
fi

LLAMA_SERVER="${LLAMA_SERVER:-llama-server}"
LOG="${LOG:-/tmp/csai-llama-chat.log}"
PID_FILE="${PID_FILE:-/tmp/csai-llama-chat.pid}"

if [[ -z "${MODEL}" || ! -f "$MODEL" ]]; then
  echo "ERROR: model GGUF not found." >&2
  echo "Set MODEL=/path/to/model.gguf or MODEL_DIR=\$HOME/LocalModels" >&2
  exit 1
fi

if lsof -nP -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Port $PORT already in use (OK if chat llama is already running)."
  lsof -nP -iTCP:"$PORT" -sTCP:LISTEN || true
  exit 0
fi

echo "Starting Chat llama-server"
echo "  model : $MODEL"
echo "  listen: http://${HOST}:${PORT}/v1"
echo "  alias : $ALIAS  ctx=$CTX  ngl=$NGL"
echo "  log   : $LOG"

nohup "$LLAMA_SERVER" \
  -m "$MODEL" \
  --host "$HOST" \
  --port "$PORT" \
  -c "$CTX" \
  -ngl "$NGL" \
  --alias "$ALIAS" \
  --api-key "$API_KEY" \
  --jinja \
  >"$LOG" 2>&1 &

echo $! >"$PID_FILE"
echo "PID $(cat "$PID_FILE")"
echo "Wait until: curl -s http://${HOST}:${PORT}/v1/models -H \"Authorization: Bearer ${API_KEY}\""
