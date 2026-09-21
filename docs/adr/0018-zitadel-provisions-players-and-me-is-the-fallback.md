# 18. Zitadel provisions players, and `/me` is the fallback that complains

- **Date:** 2026-09-17
- **Status:** accepted

## Context

Nothing works for a player until their row exists, and `GET /players/me` created it. A GET
that writes is wrong, and it makes an ordering rule every client must know. An explicit
bootstrap endpoint only renames that rule, and a Unity client would carry it too.

## Decision

We will provision players from a Zitadel event. A target calls
`POST /internal/zitadel/user-created` on service-player, and that endpoint creates the
player with its starting character. `GET /players/me` still creates a missing row and logs
ERROR on `com.tombtale.provisioning.fallback`. The endpoint sits outside `/api/v1` and an
HMAC-SHA256 signature over `<timestamp>.<body>` authorizes it, failing closed with no key.

## Consequences

- A client needs no first call at all, which a bootstrap endpoint cannot deliver.
- The event fails silently, so the fallback stays and its ERROR is the only sign that a
  delivery was lost. `logback-test.xml` silences it, because tests only take that path.
- The route is `permitAll`, so the signature is the whole guard. A fresh environment runs
  on the fallback until someone registers the target.

---

*Implemented by:* `feat(player): provision players from a Zitadel event`
