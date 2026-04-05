#!/bin/bash
# ============================================================
# Pre-Pull Request Checks
# ============================================================
# Run this script from anywhere; it resolves paths relative to
# the repository root automatically.
#
# Usage:
#   ./scripts/pre-pr-tests.sh
# ============================================================

set -e

# Resolve the repository root (one level up from scripts/)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=========================================="
echo "🚀 Running Pre-Pull Request Checks..."
echo "=========================================="

# -------------------------------------------------------
# service-commerce
# -------------------------------------------------------
COMMERCE_DIR="$REPO_ROOT/service-commerce"

if [ -d "$COMMERCE_DIR" ]; then
    echo ""
    echo "------------------------------------------"
    echo "📦 service-commerce"
    echo "------------------------------------------"

    echo "  1. 🧹 Checkstyle & PMD..."
    (cd "$COMMERCE_DIR" && ./mvnw checkstyle:check pmd:check -DskipTests)

    echo "  2. 🧪 Unit Tests & Coverage..."
    (cd "$COMMERCE_DIR" && ./mvnw clean test jacoco:report)

    echo "  ✅ service-commerce passed"
fi

# -------------------------------------------------------
# service-player
# -------------------------------------------------------
PLAYER_DIR="$REPO_ROOT/service-player"

if [ -d "$PLAYER_DIR" ]; then
    echo ""
    echo "------------------------------------------"
    echo "📦 service-player"
    echo "------------------------------------------"

    echo "  1. 🧹 Checkstyle & PMD..."
    (cd "$PLAYER_DIR" && ./mvnw checkstyle:check pmd:check -DskipTests)

    echo "  2. 🧪 Unit Tests & Coverage..."
    (cd "$PLAYER_DIR" && ./mvnw clean test jacoco:report)

    echo "  ✅ service-player passed"
fi

# -------------------------------------------------------
# frontend-portal
# -------------------------------------------------------
FRONTEND_DIR="$REPO_ROOT/frontend-portal"

if [ -d "$FRONTEND_DIR" ]; then
    echo ""
    echo "------------------------------------------"
    echo "💻 frontend-portal"
    echo "------------------------------------------"

    # Group everything in a single subshell so nvm environment changes persist
    (
        cd "$FRONTEND_DIR"

        # --- Ensure correct Node version ---
        if [ -s "${NVM_DIR:-$HOME/.nvm}/nvm.sh" ]; then
            export NVM_DIR="${NVM_DIR:-$HOME/.nvm}"
            # shellcheck source=/dev/null
            . "$NVM_DIR/nvm.sh"
            nvm use 2>/dev/null || echo "⚠️ Could not switch Node version via nvm"
        fi

        echo "  1. 📦 Production Build..."
        npm run build

        echo "  2. 🧪 Unit Tests..."
        if command -v chromium >/dev/null 2>&1; then
            export CHROME_BIN="$(command -v chromium)"
        elif command -v chromium-browser >/dev/null 2>&1; then
            export CHROME_BIN="$(command -v chromium-browser)"
        elif command -v google-chrome >/dev/null 2>&1; then
            export CHROME_BIN="$(command -v google-chrome)"
        fi
        npx ng test --watch=false --browsers=ChromeHeadless

        echo "  3. 🧹 Lint..."
        npx ng lint
    )

    echo "  ✅ frontend-portal passed"
fi

# -------------------------------------------------------
# Add future services here following the same pattern
# -------------------------------------------------------

echo ""
echo "=========================================="
echo "✅ All checks passed successfully!"
echo "You are clear to commit, push, and open the PR."
echo "=========================================="
