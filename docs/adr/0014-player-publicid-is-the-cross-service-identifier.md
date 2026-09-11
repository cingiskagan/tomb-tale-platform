# 14. `Player.publicId` is the cross-service player identifier

- **Date:** 2026-09-11
- **Status:** accepted

## Context

`Purchase.playerId` is a `String` whose Javadoc calls it the Zitadel subject.
Its seed rows hold `player-001`. So the column means the IdP's identifier, or
the application's, or neither, depending on which row you read.

Three identifiers could fill it: the Zitadel `sub`, `Player.publicId`, or the
internal `Long id` from [0003](0003-public-uuid-separate-from-the-database-key.md).
Commerce has no foreign key to choose between them — it has no rights in the
`player` schema ([0006](0006-one-postgres-schema-and-user-per-service.md)) — so
whatever it stores is a loose copy either way.

## Decision

We will make `Player.publicId` the only player identifier that crosses a service
boundary. service-player owns the mapping from Zitadel `sub` to `publicId` and
does not share the `sub`. The internal `Long id` never leaves the persistence
layer.

`Purchase.playerId` becomes a `UUID` holding a `publicId`. It arrives as an
ordinary request parameter from a caller that already has it.

## Consequences

- The Zitadel `sub` stops appearing outside service-player. Changing identity
  provider is one service's migration rather than every service's.
- The internal `Long id` was considered and rejected. Its advantages — compact
  key, insert locality, cheap joins — exist only inside a single schema, and
  commerce joins nothing. Publishing it would make the private key permanently
  public and undo 0003.
- **Commerce holds a reference it cannot validate.** Any well-formed UUID is
  accepted and stored, including one belonging to no player. Deliberate for now;
  E5 and E5a add the local replica and the lookup that close it.
- Only service-player can turn a `sub` into a `publicId`. Today that does not
  matter, because an admin names the buyer ([0011](0011-purchases-are-an-admin-tool-for-now.md)).
  A player buying for themselves arrives with a token and no `publicId`, so that
  flow is blocked until the lookup exists.
- `createdBy` on a commerce entity stays null for the same reason
  ([0013](0013-every-entity-carries-the-same-six-fields.md)).
- The frontend is already correct: it reads `publicId` from `GET /players/me`
  and uses it in player URLs. Only commerce's contract changes.

---

*Implemented by:* `docs+feat: one canonical player identity across services`
