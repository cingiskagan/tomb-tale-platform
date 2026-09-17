# 17. An API field is named after the field behind it

- **Date:** 2026-09-17
- **Status:** accepted

## Context

Two names in the purchase API did not match the entity they came from. The
response called the public identifier `id`, and it called the creation
timestamp `purchasedAt`. Both were true before [ADR 0013](0013-every-entity-carries-the-same-six-fields.md)
gave every entity the same six fields, and neither was corrected when it did:
`Purchase` gained `publicId` and `createdAt`, and the API kept the old
spellings.

Keeping them cost something in three places. The mapper needed a `@Mapping`
per name. The sort allow-list had to become a map from API name to entity name,
so `?sort=id` and `?sort=purchasedAt` still worked. And service-player, which
had always said `publicId` and `createdAt`, now disagreed with service-commerce
about what to call the same two things.

Player had the mirror-image problem: its sort allow-list accepted `id`, the
internal key, which appears in no response it sends.

## Decision

We will name every field an API exposes after the entity field behind it.

In service-commerce that means `id` becomes `publicId`, `purchasedAt` becomes
`createdAt`, the filter parameters `purchasedAfter` and `purchasedBefore`
become `createdAfter` and `createdBefore`, and the path template `/{id}`
becomes `/{publicId}`. The internal key stays out of every DTO, so `publicId`
is the only identifier an API ever names.

## Consequences

- Both mappings leave `PurchaseMapper`, and the commerce allow-list goes back
  to a `Set` of names read from the Q-type. A renamed entity field now breaks
  the build rather than the endpoint.
- `?sort=id` and `?sort=purchasedAt` are 400s. So is `?sort=id` against
  players: the internal key left that allow-list, because ordering by a column
  no response names is not something a caller can ask for in terms it can see.
- An entity rename is an API break by construction. That is the point. It means
  a rename has to be a decision rather than tidying.
- The portal is the only client and changes in the same commit, so the break is
  free. It will not be free again.
- `playerId` keeps its name although it holds another service's `publicId`. It
  names a reference to a different aggregate, the entity field is called
  `playerId` too, and [ADR 0014](0014-player-publicid-is-the-cross-service-identifier.md)
  already says what the value is.
- [ADR 0011](0011-purchases-are-an-admin-tool-for-now.md) lists the mutations
  as `POST`, `PUT`, `DELETE` on `/{id}`. They are now `POST`, `PATCH`, `DELETE`
  on `/{publicId}`. Records are append-only, so 0011 is not edited; who may
  call what is unchanged.

---

*Implemented by:* `refactor: stable pagination and update semantics`
