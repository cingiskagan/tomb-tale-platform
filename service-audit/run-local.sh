#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# run-local.sh — Start service-audit against the local Docker environment.
#
# Usage:
#   ./run-local.sh          # MongoDB and RabbitMQ via Docker
# ---------------------------------------------------------------------------
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/../infrastructure/.env"

# ── Load .env ───────────────────────────────────────────────────────────────
if [[ -f "${ENV_FILE}" ]]; then
  set -a
  # Safe key=value parser — avoids executing .env as shell code (SC1090).
  while IFS='=' read -r key value; do
    [[ -z "${key}" || "${key}" == \#* ]] && continue
    export "${key}=${value}"
  done < "${ENV_FILE}"
  set +a
else
  echo "⚠  .env file not found at ${ENV_FILE}"
fi

if [[ -z "${SERVICE_AUDIT_PORT:-}" ]]; then
  echo "❌  SERVICE_AUDIT_PORT is not set. Define it in ${ENV_FILE}" >&2
  exit 1
fi
SERVER_PORT="${SERVICE_AUDIT_PORT}"

echo "🚀  Starting service-audit (MongoDB and RabbitMQ, port ${SERVER_PORT})…"
# No platform-commons to install first: this service does not depend on it.
exec "${SCRIPT_DIR}/mvnw" spring-boot:run \
  -f "${SCRIPT_DIR}/pom.xml" \
  -Dspring-boot.run.arguments="--server.port=${SERVER_PORT}"
