#!/usr/bin/env bash
# Start local Chat model (Qwen3.6) for customer-service-ai via llama-server.
# Default port/alias match application.yml local profile: :18080 / local-qwen
set -euo pipefail

HOST="${HOST:-127.0.0.1}"
PORT="${PORT:-18080}"
CTX="${CTX:-32768}"
NGL="${NGL:-99}"
ALIAS="${ALIAS:-local-qwen}"
API_KEY="${API_KEY:-sk-local}"

# Prefer the model already used on this machine; override MODEL=... as needed
MODEL="${MODEL:-/Users/andy.yang/LocalModels/Qwen3.6-35B-A3B/Qwen3.6-35B-A3B-Uncensored-HauhauCS-Aggressive-Q6_K_P.gguf}"
if [[ ! -f "$MODEL" ]]; then
  MODEL="/Users/andy.yang/LocalModels/Qwen3.6-35B-A3B/Qwen3.6-35B-A3B-Uncensored-HauhauCS-Aggressive-IQ2_M.gguf"
fi
LLAMA_SERVER="${LLAMA_SERVER:-llama-server}"
LOG="${LOG:-/tmp/csai-llama-chat.log}"
PID_FILE="${PID_FILE:-/tmp/csai-llama-chat.pid}"

if [[ ! -f "$MODEL" ]]; then
  echo "ERROR: model not found: $MODEL" >&2
  echo "Set MODEL=/path/to/your.gguf" >&2
  exit 1
fi

if lsof -nP -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Port $PORT already in use (OK if your chat llama is already running)."
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
