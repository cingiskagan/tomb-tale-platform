#!/usr/bin/env bash
#
# PreToolUse hook: refuse the small set of actions that cannot be undone.
#
# Why this exists next to .claude/settings.json:
#   Measured 2026-09-02 in a real --dangerously-skip-permissions session: BOTH
#   `deny` rules and hooks still fire there. Bypass mode drops the prompting,
#   not the hard blocks. So this file is not the only floor.
#
#   What it adds is reach. Permission rules match a tool name and a path or a
#   command prefix. Only a hook gets the whole command string, which is the
#   only place a volume-wiping compose down or a forced push is visible at all.
#
# Why it is a substring scan and not a shell parser:
#   A denylist does not need to understand shell structure. It only asks "does
#   this dangerous thing appear anywhere", so it sees inside && chains, pipes,
#   $(...) and bash -c '...' for free. An allowlist cannot work that way, which
#   is why settings.json needs prefix rules and this file does not.
#
# What it does NOT stop:
#   Deliberate evasion. Anything that hides the command from a plain-text scan
#   -- shell variables, base64, split quoting -- walks straight through. This
#   catches honest mistakes. It is not a security boundary; that is the sandbox.
#
# Failure behaviour is OPEN, on purpose:
#   An internal error exits 1 (non-blocking) rather than 2. A bug in this file
#   should not brick a session. In normal and acceptEdits modes the deny rules
#   in settings.json are still behind it.
#
# Contract: reads the PreToolUse JSON payload on stdin.
#   exit 0 = allow, exit 2 = block (stderr goes back to Claude), exit 1 = warn.

set -uo pipefail

HOOK_NAME="block-irreversible"

payload="$(cat)"

if ! command -v jq >/dev/null 2>&1; then
    echo "${HOOK_NAME}: jq not found, cannot inspect this call -- allowing" >&2
    exit 1
fi

tool_name="$(jq -r '.tool_name // empty' <<<"$payload")"
if [ -z "$tool_name" ]; then
    echo "${HOOK_NAME}: no tool_name in payload -- allowing" >&2
    exit 1
fi

# Two different harms, so two different explanations. Destroying data and
# disclosing a credential are both worth stopping, but they are not the same
# thing and the message should not pretend otherwise.
deny() {
    echo "BLOCKED by .claude/hooks/${HOOK_NAME}.sh: $1" >&2
    echo "$2" >&2
    exit 2
}

WHY_DESTRUCTIVE="This destroys data and cannot be undone. If it is genuinely wanted, ask the user to run it by hand."
WHY_SECRET_READ="The read itself is reversible; the disclosure is not. Once a credential is in the transcript it cannot be taken back. For variable names use the .env.example file instead."
WHY_SECRET_WRITE="These credentials exist only on this machine and are not in git, so an overwrite cannot be recovered from the repository."

# Secret files in this repo, from .gitignore.
#
# The leading guard is a character class, not (^|/): inside a command the name
# is preceded by a space, so `cat .env` run from infrastructure/ has neither a
# slash nor a string start in front of it. Excluding alnum, _, . and - keeps
# `myapp.env` out. The trailing guard keeps `.env.example` out.
SECRET_EDGE='(^|[^[:alnum:]_.-])'
SECRET_RE="${SECRET_EDGE}"'\.env([^.[:alnum:]_-]|$)'
SECRET_RE+='|\.zitadel-masterkey'
SECRET_RE+='|\.personal_access_token'
SECRET_RE+="|${SECRET_EDGE}"'\.client-id'
SECRET_RE+='|\.pem([^[:alnum:]]|$)'
SECRET_RE+='|\.key([^[:alnum:]]|$)'

case "$tool_name" in
Read | Edit | Write | NotebookEdit)
    file_path="$(jq -r '.tool_input.file_path // empty' <<<"$payload")"
    if [ -n "$file_path" ] && grep -Eq "$SECRET_RE" <<<"$file_path"; then
        if [ "$tool_name" = "Read" ]; then
            deny "reading a secret file ($file_path)" "$WHY_SECRET_READ"
        fi
        deny "writing to a secret file ($file_path)" "$WHY_SECRET_WRITE"
    fi
    ;;

Bash)
    cmd="$(jq -r '.tool_input.command // empty' <<<"$payload")"
    [ -n "$cmd" ] || exit 0

    # 1. Force push -- rewrites history on the remote.
    # Broad on purpose: `git -C /repo push` and `git --no-pager push` must
    # both be seen. A false positive here only costs a manual run.
    if grep -Eq '(^|[^[:alnum:]_-])git[[:space:]].*push' <<<"$cmd"; then
        if grep -Eq -- '(^|[[:space:]])(--force|--force-with-lease|--force-if-includes|-f)([[:space:]=]|$)' <<<"$cmd"; then
            deny "force push" "$WHY_DESTRUCTIVE"
        fi
        if grep -Eq 'push[^|;&]*[[:space:]]\+[^[:space:]]+:' <<<"$cmd"; then
            deny "force push via a +refspec" "$WHY_DESTRUCTIVE"
        fi
    fi

    # 2. `docker compose down -v` -- wipes the volumes, taking Zitadel's org,
    #    project, roles and OAuth client with them. Slow to rebuild by hand.
    #    Anything may sit between `compose` and `down`, because options that
    #    take a value (-f <file>, -p <name>) put a non-flag token in between.
    #    Stopping at | ; & keeps it inside one command.
    if grep -Eq 'docker([[:space:]]+compose|-compose)[^|;&]*[[:space:]]down' <<<"$cmd"; then
        if grep -Eq -- '(^|[[:space:]])(--volumes|-[[:alpha:]]*v)([[:space:]]|$)' <<<"$cmd"; then
            deny "docker compose down with volume removal" "$WHY_DESTRUCTIVE"
        fi
    fi

    # 3. Recursive forced delete, in any flag spelling or order.
    if grep -Eq '(^|[^[:alnum:]_./-])rm[[:space:]]' <<<"$cmd" &&
        grep -Eq -- '(^|[[:space:]])-[[:alpha:]]*[rR]|--recursive' <<<"$cmd" &&
        grep -Eq -- '(^|[[:space:]])-[[:alpha:]]*[fF]|--force' <<<"$cmd"; then
        deny "recursive forced delete (rm -rf)" "$WHY_DESTRUCTIVE"
    fi

    # 4. Secrets read through the shell, which the Read rule above cannot see.
    #    `grep` is deliberately absent: searching for the string ".env" in a
    #    file like .gitignore is common and harmless, and excluding it costs
    #    less than the false positives would.
    if grep -Eq '(^|[^[:alnum:]_./-])(cat|less|more|head|tail|bat|xxd|od|strings|nl|base64|cp|scp|rsync)[[:space:]]' <<<"$cmd" &&
        grep -Eq "$SECRET_RE" <<<"$cmd"; then
        deny "a shell command targets a secret file" "$WHY_SECRET_READ"
    fi
    ;;
esac

exit 0
