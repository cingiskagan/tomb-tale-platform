# 19. Zitadel configuration is code

- **Date:** 2026-09-18
- **Status:** accepted

## Context

Everything this platform needs from Zitadel was created by clicking: the project, its three
roles, the portal's OIDC client. Nothing recorded the clicks, and rebuilding a wiped
instance was impossible. [ADR 0018](0018-zitadel-provisions-players-and-me-is-the-fallback.md)
then added a target and an execution, two more things to click and to forget.

## Decision

We will create every Zitadel object from `infrastructure/zitadel-setup.sh`, which is
idempotent and talks to the REST API. An `IAM_OWNER` service account is seeded at instance
init, so no human mints a credential. The script writes the generated client id and signing
key back where they are consumed, then revokes its own token.

## Consequences

- A wiped instance rebuilds in about a minute, done three times to prove it. Setup is
  one-shot: the token is revoked, so a second run needs one by hand or a new instance.
- `IAM_OWNER` is needed for one step, because Actions targets are instance-level. Until
  the revocation lands, that token sits unencrypted in a Docker volume.
- There is no drift detection. The script records what we asked for, not what is there.

---

*Implemented by:* `feat(infra): configure Zitadel from code`
