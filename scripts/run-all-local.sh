#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# run-all-local.sh — Start the Spring Boot services through their run-local.sh.
#
# Usage:
#   ./scripts/run-all-local.sh                    # all four
#   ./scripts/run-all-local.sh player inventory   # only these, the rest in a debugger
#
# Each service starts once the one before it listens. Three of them install
# platform-commons first, and three builds at once would race over its jar.
# Ctrl+C stops them all.
# ---------------------------------------------------------------------------
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/infrastructure/.env"
STARTUP_TIMEOUT_SECONDS=180
SERVICES=("player" "commerce" "inventory" "audit")

env_value() {
  sed -n "s/^$1=//p" "${ENV_FILE}" 2>/dev/null | tail -n 1
}

port_answers() {
  (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null
}

SELECTED=("$@")
if [[ "${#SELECTED[@]}" -eq 0 ]]; then
  SELECTED=("${SERVICES[@]}")
fi
for name in "${SELECTED[@]}"; do
  if [[ " ${SERVICES[*]} " != *" ${name} "* ]]; then
    echo "❌  Unknown service: ${name}. Valid: ${SERVICES[*]}" >&2
    exit 1
  fi
done

# Each service runs in a process group of its own, so killing the group also
# stops the JVM that spring-boot:run forks.
PIDS=()
# shellcheck disable=SC2317,SC2329  # reached only through the trap
stop_all() {
  trap - EXIT
  for pid in "${PIDS[@]}"; do
    kill -TERM -- "-${pid}" 2>/dev/null || true
  done
  wait 2>/dev/null || true
}
trap stop_all EXIT
trap 'exit 130' INT TERM

for name in "${SELECTED[@]}"; do
  port="$(env_value "SERVICE_${name^^}_PORT")"
  if [[ -z "${port}" ]]; then
    echo "❌  SERVICE_${name^^}_PORT is not set. Define it in ${ENV_FILE}" >&2
    exit 1
  fi
  if port_answers "${port}"; then
    echo "❌  Port ${port} is taken, so service-${name} is already running or something else holds it." >&2
    exit 1
  fi

  # The prefixer ignores Ctrl+C, so the services' shutdown lines still print.
  setsid "${ROOT_DIR}/service-${name}/run-local.sh" \
    > >(trap '' INT; exec sed -u "s/^/[${name}] /") 2>&1 &
  pid=$!
  PIDS+=("${pid}")

  waited=0
  until port_answers "${port}"; do
    if ! kill -0 "${pid}" 2>/dev/null; then
      echo "❌  service-${name} stopped before it listened on ${port}." >&2
      exit 1
    fi
    if (( waited >= STARTUP_TIMEOUT_SECONDS )); then
      echo "❌  service-${name} did not listen on ${port} within ${STARTUP_TIMEOUT_SECONDS}s." >&2
      exit 1
    fi
    sleep 2
    waited=$((waited + 2))
  done
  echo "✅  service-${name} listens on ${port}"
done

echo "🟢  ${SELECTED[*]} running. Ctrl+C stops them all."
status=0
wait -n "${PIDS[@]}" || status=$?
echo "⚠  A service stopped (exit ${status}), so the rest stop too." >&2
exit "${status}"
