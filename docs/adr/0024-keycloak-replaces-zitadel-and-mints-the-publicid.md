# 24. Keycloak replaces Zitadel, and it mints the player's `publicId`

- **Date:** 2026-10-06
- **Status:** accepted

## Context

Zitadel has no default roles and cannot create an id with a user, so
service-player wrote the `publicId` and granted the `player` role through
Zitadel's API. Each fix to that sync opened a new gap.

## Decision

We will run Keycloak from `infrastructure/keycloak/import/tombtale-realm.json`,
with `player` as the default role. Our `public-id-mapper` gives each user a UUID
of its own on the first token, and the token carries it as `public_id`.
service-player creates the row from that claim on the first `POST /players/me`
and never calls Keycloak. Every login takes the password, then an emailed code.

## Consequences

- No provisioning code, no write-back and no reconciliation remain.
- Supersedes 0002, 0018, 0019 and 0023. ADR 0014 holds: Keycloak's user id
  stays in service-player's `iam_id`, and players cannot edit `public_id`.
- The emailed code is a third-party extension that trails Keycloak releases.
- A realm change needs the realm deleted and imported again.

---

*Implemented by:* `feat: the player comes from the Keycloak token`
