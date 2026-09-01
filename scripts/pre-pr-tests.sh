#!/bin/bash
# ============================================================
# Pre-Pull Request Checks
# ============================================================
# Run this script from anywhere; it resolves paths relative to
# the repository root automatically.
#
# Usage:
#   ./scripts/pre-pr-tests.sh                          # everything
#   ./scripts/pre-pr-tests.sh --scope general          # shellcheck, markdownlint, yamllint
#   ./scripts/pre-pr-tests.sh --scope service-player   # one module
#   ./scripts/pre-pr-tests.sh --clean                  # also runs npm ci
#
# --scope takes exactly one of: all (default), general, service-commerce,
# service-player, frontend-portal. An unrecognised value is an error rather
# than a silent no-op, so a typo cannot look like a clean run.
# ============================================================

set -e

CLEAN_INSTALL=false
SCOPE="all"

# Pinned so every machine runs the same rules. MegaLinter builds its own image
# and does not publish which markdownlint it bundles, so this is not a guarantee
# of exact CI parity — bump it if CI ever reports a rule this version lacks.
MARKDOWNLINT_VERSION="0.45.0"

# yamllint ships no npm package, so the fallback here is pipx rather than npx.
# The rules come from .yamllint.yml, which MegaLinter reads too — that file,
# not this pin, is what keeps local runs and CI agreeing.
YAMLLINT_VERSION="1.35.1"

VALID_SCOPES=("all" "general" "service-commerce" "service-player" "frontend-portal")

while [[ "$#" -gt 0 ]]; do
    case $1 in
        --clean) CLEAN_INSTALL=true; shift ;;
        --scope) SCOPE="$2"; shift 2 ;;
        --scope=*) SCOPE="${1#*=}"; shift ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
done

# Reject a scope we do not know. Without this a typo selects nothing, every
# section is skipped, and the script still prints "All checks passed".
scope_is_valid=false
for valid_scope in "${VALID_SCOPES[@]}"; do
    [ "$SCOPE" = "$valid_scope" ] && scope_is_valid=true
done
if [ "$scope_is_valid" = false ]; then
    echo "Unknown scope: $SCOPE"
    echo "Valid scopes: ${VALID_SCOPES[*]}"
    exit 1
fi

# True when the requested scope covers the given section.
in_scope() {
    [ "$SCOPE" = "all" ] || [ "$SCOPE" = "$1" ]
}

# Resolve the repository root (one level up from scripts/)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=========================================="
echo "🚀 Running Pre-Pull Request Checks..."
echo "=========================================="

if in_scope general; then
    echo ""
    echo "------------------------------------------"
    echo "🛠️ General Checks"
    echo "------------------------------------------"
    if command -v shellcheck >/dev/null 2>&1; then
        echo "  1. 🐚 ShellCheck..."
        shellcheck "$SCRIPT_DIR/"*.sh
        echo "  ✅ Shell files passed"
    else
        echo "  ⚠️ shellcheck not installed locally, skipping local validation"
    fi

    # Markdown is linted in CI by MegaLinter, and on pushes to main it lints
    # every file in the repo rather than only the changed ones. Checking here
    # keeps a formatting slip from turning main red after a merge.
    echo "  2. 📝 Markdownlint..."
    # Lint exactly the files git tracks, which is what CI checks out. Globbing
    # the working tree instead would also flag ignored files such as
    # docs/reviews and service-player/HELP.md that MegaLinter never sees.
    mapfile -t MD_FILES < <(git -C "$REPO_ROOT" ls-files '*.md')

    # Resolve the pinned version first. A global markdownlint is used only when
    # it already matches the pin, otherwise whatever version happens to be
    # installed would quietly decide the rule set instead.
    MARKDOWNLINT_CMD=()
    if command -v markdownlint >/dev/null 2>&1 &&
        [ "$(markdownlint --version 2>/dev/null)" = "$MARKDOWNLINT_VERSION" ]; then
        MARKDOWNLINT_CMD=(markdownlint)
    elif command -v npx >/dev/null 2>&1; then
        MARKDOWNLINT_CMD=(npx --yes "markdownlint-cli@$MARKDOWNLINT_VERSION")
    fi

    if [ ${#MD_FILES[@]} -eq 0 ]; then
        echo "  ⚠️ no tracked markdown files found, skipping markdown validation"
    elif [ ${#MARKDOWNLINT_CMD[@]} -eq 0 ]; then
        echo "  ⚠️ no npx and no markdownlint $MARKDOWNLINT_VERSION, skipping markdown validation"
    else
        (cd "$REPO_ROOT" && "${MARKDOWNLINT_CMD[@]}" "${MD_FILES[@]}")
        echo "  ✅ Markdown files passed"
    fi

    # YAML is linted in CI by MegaLinter. A Spring profile with the wrong
    # indentation still parses and still runs the tests, so nothing else in
    # this script would notice it.
    echo "  3. 📐 Yamllint..."
    # Same file list CI sees: tracked YAML, minus MegaLinter's FILTER_REGEX_EXCLUDE.
    mapfile -t YAML_FILES < <(git -C "$REPO_ROOT" ls-files '*.yml' '*.yaml' | grep -v '^\.mvn/')

    YAMLLINT_CMD=()
    if command -v yamllint >/dev/null 2>&1 &&
        [ "$(yamllint --version 2>/dev/null)" = "yamllint $YAMLLINT_VERSION" ]; then
        YAMLLINT_CMD=(yamllint)
    elif command -v pipx >/dev/null 2>&1; then
        YAMLLINT_CMD=(pipx run "yamllint==$YAMLLINT_VERSION")
    fi

    if [ ${#YAML_FILES[@]} -eq 0 ]; then
        echo "  ⚠️ no tracked YAML files found, skipping YAML validation"
    elif [ ${#YAMLLINT_CMD[@]} -eq 0 ]; then
        echo "  ⚠️ no pipx and no yamllint $YAMLLINT_VERSION, skipping YAML validation"
    else
        (cd "$REPO_ROOT" && "${YAMLLINT_CMD[@]}" "${YAML_FILES[@]}")
        echo "  ✅ YAML files passed"
    fi
fi

# -------------------------------------------------------
# service-commerce
# -------------------------------------------------------
COMMERCE_DIR="$REPO_ROOT/service-commerce"

if [ -d "$COMMERCE_DIR" ] && in_scope service-commerce; then
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

if [ -d "$PLAYER_DIR" ] && in_scope service-player; then
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

if [ -d "$FRONTEND_DIR" ] && in_scope frontend-portal; then
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

        echo "  1. 📦 Dependencies & Production Build..."
        if [ "$CLEAN_INSTALL" = true ]; then
            if [ -f package-lock.json ]; then
                npm ci
            else
                npm install
            fi
        else
            echo "      (Skipping dependency install. Run with --clean to enforce.)"
        fi
        npm run build

        echo "  2. 🧪 Unit Tests..."
        # Karma needs the real browser executable. On snap systems
        # /snap/bin/chromium is a symlink to /usr/bin/snap, the launcher: Karma
        # spawns it, snap starts the browser in a separate process tree, the
        # launcher exits, and Karma reports "crashed" while the orphaned browser
        # keeps holding port 9222. Every candidate is screened, because an
        # exported CHROME_BIN and a PATH lookup can each resolve to the launcher.
        is_snap_launcher() {
            local resolved
            case "$1" in
                /snap/bin/*) return 0 ;;
            esac
            resolved="$(readlink -f "$1" 2>/dev/null || true)"
            if [ -n "$resolved" ] && [ "$(basename "$resolved")" = "snap" ]; then
                return 0
            fi
            return 1
        }

        # Preference order: an explicit CHROME_BIN, the real binary inside the
        # snap, then whatever is on PATH.
        SNAP_CHROME="/snap/chromium/current/usr/lib/chromium-browser/chrome"
        CHROME_CANDIDATES=()
        if [ -n "${CHROME_BIN:-}" ]; then
            CHROME_CANDIDATES+=("$CHROME_BIN")
        fi
        CHROME_CANDIDATES+=("$SNAP_CHROME")
        for chrome_name in chromium chromium-browser google-chrome; do
            chrome_path="$(command -v "$chrome_name" 2>/dev/null || true)"
            if [ -n "$chrome_path" ]; then
                CHROME_CANDIDATES+=("$chrome_path")
            fi
        done

        CHROME_BIN=""
        for chrome_candidate in "${CHROME_CANDIDATES[@]}"; do
            if [ ! -x "$chrome_candidate" ]; then
                continue
            fi
            if is_snap_launcher "$chrome_candidate"; then
                echo "      (ignoring snap launcher: $chrome_candidate)"
                continue
            fi
            CHROME_BIN="$chrome_candidate"
            break
        done

        if [ -z "$CHROME_BIN" ]; then
            echo "❌ CRITICAL: No usable Chromium/Chrome binary found for CHROME_BIN." >&2
            echo "   Note that /snap/bin/chromium is a launcher, not the browser," >&2
            echo "   and Karma cannot drive it. Install chromium-browser or" >&2
            echo "   google-chrome, or point CHROME_BIN at a real binary." >&2
            exit 1
        fi
        export CHROME_BIN
        echo "      Using CHROME_BIN=$CHROME_BIN"
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
