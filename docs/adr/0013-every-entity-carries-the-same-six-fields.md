# 13. Every entity carries the same six fields

- **Date:** 2026-09-11
- **Status:** accepted

## Context

`Player` and `GameCharacter` agree on `id`, `publicId`, `createdAt` and `updatedAt`.
`Purchase` agrees with none of it, and neither service records who acted, although
[0011](0011-purchases-are-an-admin-tool-for-now.md) makes an admin buy for a player.

## Decision

We will give every entity the same six fields in one `BaseEntity`, a `@MappedSuperclass`
in `platform-commons`: `Long id` inside, `UUID publicId` as the only identifier an API
names ([0003](0003-public-uuid-separate-from-the-database-key.md)), `createdAt`,
`updatedAt`, and `createdBy`/`updatedBy` holding the player or `SystemActor` that wrote.

## Consequences

- `Purchase` changes shape: the UUID key becomes `publicId`, a new `Long id` takes over,
  `purchasedAt` goes, and the API still exchanges the same UUID.
- `createdBy` stays null until commerce can turn a token into a `publicId`. Then it
  rejects any write it cannot attribute, and the column tightens to `NOT NULL`.
- Lombok follows: `@SuperBuilder` and explicit `@Getter`/`@Setter`, because `BaseEntity`
  defines `equals` on `publicId`. Every table pays two columns and a unique index.

---

*Implemented by:* `refactor: a shared module and one base entity`, then `docs+feat: one canonical player identity across services`
