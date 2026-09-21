# 14. `Player.publicId` is the cross-service player identifier

- **Date:** 2026-09-11
- **Status:** accepted

## Context

`Purchase.playerId` is a `String` whose Javadoc calls it the Zitadel subject, while its
seed rows hold `player-001`. Three identifiers fit it: the `sub`, `Player.publicId`,
or the internal `Long id` from [0003](0003-public-uuid-separate-from-the-database-key.md).

## Decision

We will make `Player.publicId` the only player identifier that crosses a service
boundary. service-player owns the mapping from Zitadel `sub` to `publicId` and does not
share it. `Purchase.playerId` becomes a `UUID` holding a `publicId`.

## Consequences

- The `sub` stops appearing outside service-player, so a provider change is one service's
  migration. The internal `Long id` was rejected: it is compact only inside one schema.
- Commerce holds a reference it cannot validate. Any well-formed UUID is stored, and
  `createdBy` stays null ([0013](0013-every-entity-carries-the-same-six-fields.md)).
- Only service-player turns a `sub` into a `publicId`, so a player buying for themselves
  waits for that lookup, while an admin names the buyer
  ([0011](0011-purchases-are-an-admin-tool-for-now.md)).

---

*Implemented by:* `docs+feat: one canonical player identity across services`
