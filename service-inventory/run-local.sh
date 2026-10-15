#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# run-local.sh — Start service-inventory against the local Docker environment.
#
# Usage:
#   ./run-local.sh          # Postgres and RabbitMQ via Docker
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

if [[ -z "${SERVICE_INVENTORY_PORT:-}" ]]; then
  echo "❌  SERVICE_INVENTORY_PORT is not set. Define it in ${ENV_FILE}" >&2
  exit 1
fi
SERVER_PORT="${SERVICE_INVENTORY_PORT}"

echo "🚀  Starting service-inventory (Postgres and RabbitMQ, port ${SERVER_PORT})…"
# Installs platform-commons first, as service-commerce/run-local.sh explains.
"${SCRIPT_DIR}/../mvnw" -q -f "${SCRIPT_DIR}/../pom.xml" -pl platform-commons install -DskipTests

exec "${SCRIPT_DIR}/mvnw" spring-boot:run \
  -f "${SCRIPT_DIR}/pom.xml" \
  -Dspring-boot.run.arguments="--server.port=${SERVER_PORT}"
