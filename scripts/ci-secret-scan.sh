#!/usr/bin/env bash
# Fail CI if tracked sources look like they contain real secrets.
# Prints only masked evidence (never full secret values).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# High-risk patterns (not demo placeholders).
PATTERNS=(
  'sk-[a-zA-Z0-9]{24,}'
  'sk-proj-[A-Za-z0-9_-]{20,}'
  'sk-ant-[A-Za-z0-9_-]{20,}'
  'dataset-[A-Za-z0-9]{16,}'
  'gh[pousr]_[A-Za-z0-9]{20,}'
  'github_pat_[A-Za-z0-9_]{20,}'
  'AKIA[0-9A-Z]{16}'
  '-----BEGIN [A-Z0-9 ]*PRIVATE KEY-----'
)

ALLOW='sk-local|sk-placeholder|sk-xxx|change-me|mock-key|local-dev-|csai-client|csai-admin'

fail=0
while IFS= read -r file; do
  [[ -z "$file" || ! -f "$file" ]] && continue
  # Skip binary-ish / large assets
  case "$file" in
    *.png|*.jpg|*.jpeg|*.gif|*.webp|*.pdf|*.jar|*.class|*.gguf) continue ;;
  esac
  for pat in "${PATTERNS[@]}"; do
    # Use grep -E; skip allowed demo lines
    while IFS= read -r line; do
      [[ -z "$line" ]] && continue
      if echo "$line" | grep -Eqi "$ALLOW"; then
        continue
      fi
      # Mask value-ish tokens
      masked="$(echo "$line" | sed -E 's/(sk-|dataset-|gh[pousr]_|github_pat_|AKIA)[A-Za-z0-9._-]{8,}/\1***/g' | cut -c1-160)"
      echo "RISK  ${file}: ${masked}"
      fail=1
    done < <(grep -nE "$pat" "$file" 2>/dev/null || true)
  done
done < <(git ls-files)

if [[ "$fail" -ne 0 ]]; then
  echo
  echo "CI secret hygiene FAILED: remove secrets from the commit (use .env locally)."
  exit 1
fi

echo "OK  secrets-hygiene: no high-risk patterns in tracked files"
