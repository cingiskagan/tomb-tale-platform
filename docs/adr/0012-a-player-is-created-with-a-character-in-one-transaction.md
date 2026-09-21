# 12. A player is created with a character, in one transaction

- **Date:** 2026-09-09
- **Status:** accepted

## Context

`GET /players/me` was a write: it saved a default character whenever it found a
player with none. Those rows were real under `ddl-auto: update`. Since
[0005](0005-flyway-owns-the-schema.md) and
[0006](0006-one-postgres-schema-and-user-per-service.md) every database starts
empty, so the backfill repaired nothing and stood in for an unchecked guarantee.

## Decision

We will treat *a player owns at least one character* as an invariant of the creation
path, held by one transaction and never repaired on read.
`createNewPlayerWithCharacter` saves both rows through one `playerRepository.save` with
`cascade = CascadeType.ALL`, so both land or neither does.

## Consequences

- A characterless player is now possible and nothing repairs it. The failure
  becomes visible instead of healed on the next login, which is the trade.
- Any new path that creates a `Player` creates its character in the same
  transaction. `PlayerCreationAtomicityTest` commits real rows to prove it.

---

*Implemented by:* `refactor(player): remove the login-time character backfill`
