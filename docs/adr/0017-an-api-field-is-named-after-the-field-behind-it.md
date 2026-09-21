# 17. An API field is named after the field behind it

- **Date:** 2026-09-17
- **Status:** accepted

## Context

The purchase API called the public identifier `id` and the creation timestamp `purchasedAt`,
names the entity stopped using when [0013](0013-every-entity-carries-the-same-six-fields.md)
gave it `publicId` and `createdAt`. The mapper and the sort allow-list both paid for it.

## Decision

We will name every field an API exposes after the entity field behind it. In commerce `id`
becomes `publicId`, `purchasedAt` becomes `createdAt`, `purchasedAfter` and
`purchasedBefore` become `createdAfter` and `createdBefore`, and `/{id}` becomes
`/{publicId}`. The internal key stays out of every DTO.

## Consequences

- The per-name `@Mapping` entries leave `PurchaseMapper`, and the sort allow-list is a `Set`
  read from the Q-type again, so a renamed entity field breaks the build, not the endpoint.
- `?sort=id` and `?sort=purchasedAt` are 400s, and so is `?sort=id` against players. The
  portal is the only client and changes in the same commit, so the break is free this once.
- `playerId` keeps its name ([0014](0014-player-publicid-is-the-cross-service-identifier.md)),
  and [0011](0011-purchases-are-an-admin-tool-for-now.md) keeps its older spellings.

---

*Implemented by:* `refactor: stable pagination and update semantics`
