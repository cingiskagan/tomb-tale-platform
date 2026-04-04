#!/usr/bin/env bash
# ===========================================================
# Tomb Tale Frontend Portal — Automated Test Runner
# ===========================================================
# Usage: ./automated-tests.sh
# ===========================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "============================================="
echo " Tomb Tale Frontend Portal — Test Suite"
echo "============================================="

# --- Ensure correct Node version ---
if command -v nvm &>/dev/null || [ -s "$HOME/.nvm/nvm.sh" ]; then
  export NVM_DIR="${NVM_DIR:-$HOME/.nvm}"
  # shellcheck source=/dev/null
  . "$NVM_DIR/nvm.sh"
  nvm use 2>/dev/null || echo "⚠ Could not switch Node version via nvm"
fi

echo ""
echo "▶ Node: $(node --version)"
echo "▶ npm:  $(npm --version)"
echo ""

# --- Build check ---
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Step 1/3: Production Build"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
npm run build --prefix "$SCRIPT_DIR"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Step 2/3: Unit Tests"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
export CHROME_BIN=/snap/bin/chromium
npx --prefix "$SCRIPT_DIR" ng test --watch=false --browsers=ChromeHeadless

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Step 3/3: Lint"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
npx --prefix "$SCRIPT_DIR" ng lint

echo ""
echo "============================================="
echo " ✅ All checks passed!"
echo "============================================="
