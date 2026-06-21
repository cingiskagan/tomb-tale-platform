#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# run-local.sh — Start service-player against the local Docker environment.
#
# Usage:
#   ./run-local.sh          # Full mode (Postgres + RabbitMQ via Docker)
#   ./run-local.sh --test   # Test mode  (H2 in-memory, no Docker deps)
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

if [[ -z "${SERVICE_PLAYER_PORT:-}" ]]; then
  echo "❌  SERVICE_PLAYER_PORT is not set. Define it in ${ENV_FILE}" >&2
  exit 1
fi
SERVER_PORT="${SERVICE_PLAYER_PORT}"

# ── Test mode (H2 in-memory, no external dependencies) ─────────────────────
if [[ "${1:-}" == "--test" ]]; then
  echo "🧪  Starting service-player in TEST mode (H2, port ${SERVER_PORT})…"
  exec "${SCRIPT_DIR}/mvnw" spring-boot:test-run \
    -f "${SCRIPT_DIR}/pom.xml" \
    -Dspring-boot.run.profiles=test \
    -Dspring-boot.run.arguments="--server.port=${SERVER_PORT}"
fi

# ── Full mode (real Postgres + RabbitMQ) ────────────────────────────────────
echo "🚀  Starting service-player in FULL mode (Postgres, port ${SERVER_PORT})…"
exec "${SCRIPT_DIR}/mvnw" spring-boot:run \
  -f "${SCRIPT_DIR}/pom.xml" \
  -Dspring-boot.run.arguments="--server.port=${SERVER_PORT}"
