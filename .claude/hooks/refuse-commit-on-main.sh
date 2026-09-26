#!/usr/bin/env bash
# PreToolUse hook: denies a git commit while the branch is main or master.
# Reads the Bash tool call as JSON on stdin. Any other command passes.
set -euo pipefail

command=$(jq -r '.tool_input.command // empty')

# git, then any options before the subcommand (-C and -c take a value), then commit.
commit_re='(^|[;&|({[:space:]])git([[:space:]]+(-[Cc][[:space:]]+[^[:space:]]+|-[^[:space:]]+))*[[:space:]]+commit([[:space:]]|$)'
[[ $command =~ $commit_re ]] || exit 0

branch=$(git branch --show-current 2>/dev/null || true)
if [[ $branch == main || $branch == master ]]; then
  jq -n --arg reason "You are on $branch. Create a branch first, then commit." \
    '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: $reason}}'
fi
