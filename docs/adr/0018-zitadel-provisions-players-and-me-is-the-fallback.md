# 18. Zitadel provisions players, and `/me` is the fallback that complains

- **Date:** 2026-09-17
- **Status:** accepted

## Context

Nothing in the platform works for a player until their row exists, and
`GET /players/me` was what created it. Two problems, both named in the
architecture review as R5. A GET that writes is wrong on its own terms. And it
makes an ordering rule every client has to know: call `/me` first, or every
other endpoint answers 404.

The obvious fix — an explicit `POST /players/me/bootstrap` — does not remove
that rule. It renames it. Every client still has to call something before
anything else works, and now every client carries the call. With a Unity client
coming (F1), that is the same rule duplicated per platform.

Zitadel already knows the moment a user is created, and it can call out. Its
Actions v2 targets exist for this.

## Decision

We will provision players from a Zitadel event. A target calls
`POST /internal/zitadel/user-created` on service-player when a user is created,
and that endpoint creates the player with its starting character.

`GET /players/me` keeps creating the row when it finds none, and logs ERROR on
the `com.tombtale.provisioning.fallback` category when it does.

The endpoint sits outside `/api/v1` because it is a private integration
surface, not published API. It carries no user token, so an HMAC-SHA256
signature over `<timestamp>.<body>` authorises it instead, verified by
`ZitadelSignatureVerifier` and failing closed when no key is configured.

## Consequences

- A client needs no first call. Any endpoint works whenever it is called, which
  is the half of R5 a bootstrap endpoint could not deliver.
- The event fails silently. A target never registered in a fresh environment
  behaves exactly like one that works — people log in, and only the missing
  rows show it. So the fallback stays: without it, one lost delivery leaves a
  Zitadel user with nothing behind them, permanently. The ERROR is the only
  thing that distinguishes the two states, which is why it is an error and not
  a debug line.
- That alarm is off in tests, in `logback-test.xml`. There is no Zitadel in a
  test run, so the fallback is the only path a test can take, and shouting on
  every build is how an alarm gets ignored.
- The route is `permitAll` in the filter chain. The signature is the entire
  guard, so a blank key refuses every call rather than accepting any. Traefik
  routes only `/api/v1/players` and `/api/v1/purchases`, so the prefix is not
  reachable from outside the network either.
- Zitadel's configuration is still hand-clicked, and this adds one more click.
  A fresh environment quietly runs on the fallback until someone registers the
  target. That is the cost of having no Zitadel config as code, which is now on
  the backlog.
- Services run on the host rather than in the Compose stack, so `zitadel-api`
  gained `extra_hosts: host.docker.internal:host-gateway` and the target URL is
  `http://host.docker.internal:8081/internal/zitadel/user-created`. Both change
  if the services ever move into the stack.
- The field carrying the user id is not pinned. The controller tries `userID`,
  `userId` and `aggregateID`, and a payload with none of them comes back as 400
  naming the fields it did carry. Pin it against a captured payload once the
  target has fired for real.
- Users that existed in Zitadel before the target was registered never fire the
  event. They are provisioned by the fallback on their next login, with an
  error logged each time until they have a row.

---

*Implemented by:* `feat(player): provision players from a Zitadel event`
