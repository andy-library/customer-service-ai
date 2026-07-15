#!/usr/bin/env bash
# Expose in-cluster Dify API/Web to localhost for Spring AI + browser.
# API:  http://127.0.0.1:15001/v1   (use as DIFY_BASE_URL)
# Web:  http://127.0.0.1:18090/
set -euo pipefail

export PATH="/usr/bin:/bin:/usr/sbin:/sbin:/opt/homebrew/bin:${PATH:-}"

API_LOCAL_PORT="${API_LOCAL_PORT:-15001}"
WEB_LOCAL_PORT="${WEB_LOCAL_PORT:-18090}"
NS="${DIFY_NS:-dify}"

ensure_pf() {
  local name="$1"
  local svc="$2"
  local local_port="$3"
  local remote_port="$4"
  local log="/tmp/csai-dify-${name}.log"
  local pidf="/tmp/csai-dify-${name}.pid"

  if lsof -nP -iTCP:"${local_port}" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "OK  ${name}: already listening on :${local_port}"
    return 0
  fi

  if ! kubectl get svc -n "${NS}" "${svc}" >/dev/null 2>&1; then
    echo "FAIL ${name}: kubectl cannot see svc/${svc} in ns/${NS}"
    return 1
  fi

  nohup kubectl -n "${NS}" port-forward "svc/${svc}" "${local_port}:${remote_port}" \
    >"${log}" 2>&1 &
  echo $! >"${pidf}"
  sleep 1
  if lsof -nP -iTCP:"${local_port}" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "OK  ${name}: port-forward started pid=$(cat "${pidf}") :${local_port}->${remote_port}"
  else
    echo "FAIL ${name}: see ${log}"
    cat "${log}" || true
    return 1
  fi
}

ensure_pf api api "${API_LOCAL_PORT}" 5001
ensure_pf web dify-nginx "${WEB_LOCAL_PORT}" 80

echo
echo "DIFY_BASE_URL=http://127.0.0.1:${API_LOCAL_PORT}/v1"
echo "Console UI:   http://127.0.0.1:${WEB_LOCAL_PORT}/"
curl -sS -o /dev/null -w "api_health HTTP %{http_code}\n" "http://127.0.0.1:${API_LOCAL_PORT}/health" || true
