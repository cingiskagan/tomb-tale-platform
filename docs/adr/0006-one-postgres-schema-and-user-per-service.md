# 6. One Postgres schema and user per service

- **Date:** 2026-08-29
- **Status:** accepted

## Context

Both services shared the `public` schema, and `init-db.sh` gave each of them
`ALL PRIVILEGES` on it, so either had the rights to drop the other's tables.
Sharing one schema also forced three Flyway workarounds.

## Decision

We will keep one Postgres instance and give each service its own user and its
own schema, owned by that user. `svc_player` owns `player`, `svc_commerce` owns
`commerce`, and neither has any rights in `public`.

## Consequences

- Postgres enforces the isolation now, so one service cannot reach the other's
  tables. Flyway migrates a schema it owns, so the three settings in
  [0005](0005-flyway-owns-the-schema.md) are deleted.
- This isolates data, not infrastructure: one instance, one connection limit.
- `V1` dropped and rebuilt the tables instead of moving them, a shortcut that
  needed an undeployed database and is now spent. `default_schema` reached the
  tests too, until [0007](0007-testcontainers-instead-of-mocked-repositories.md).

---

*Implemented by:* `33c41ea` feat(infra): give each service its own Postgres schema
