# 13. Every entity carries the same six fields

- **Date:** 2026-09-11
- **Status:** accepted

## Context

`Player` and `GameCharacter` agree: `Long id`, `UUID publicId`, `createdAt`,
`updatedAt`. `Purchase` agrees with none of it — a `UUID` primary key, no
`publicId`, `purchasedAt` in place of `createdAt`, no auditing at all.

Neither service records *who* acted. [0011](0011-purchases-are-an-admin-tool-for-now.md)
makes an admin create purchases on behalf of a player, so the actor and the
subject are now different people and only the subject is stored.

## Decision

We will give every JPA entity the same six fields:

| Field | Type | Purpose |
| --- | --- | --- |
| `id` | `Long`, IDENTITY | internal key, never leaves the persistence layer |
| `publicId` | `UUID`, unique, not updatable | the only identifier in APIs |
| `createdAt` / `updatedAt` | `Instant` | Spring Data `@CreatedDate` / `@LastModifiedDate` |
| `createdBy` / `updatedBy` | `UUID`, nullable | the acting principal, via `AuditorAware` |

A principal is whoever caused the write: a player, identified by `publicId`, or
a service or scheduled job, identified by a fixed constant in a `SystemActor`
class. Those constants are hardcoded and identical in every environment.

They live in one `BaseEntity`, a `@MappedSuperclass` in the `platform-commons`
module, and every entity extends it. `id` and `publicId` follow
[0003](0003-public-uuid-separate-from-the-database-key.md), except that
`publicId` is assigned where it is declared rather than in `@PrePersist`, so it
exists before the row is saved. Both services enable `@EnableJpaAuditing` and
supply an `AuditorAware`.

## Consequences

- `Purchase` changes shape. Today its primary key *is* the public UUID. That
  UUID moves to `publicId`, and a new `Long id` — `bigint` in Postgres — becomes
  the primary key. Audit columns are added. The API still exchanges the same
  UUID, so no client changes.
- `purchasedAt` and `createdAt` mean the same thing today. `purchasedAt` is
  dropped rather than kept as a duplicate.
- **`createdBy` is nullable, and only until commerce can resolve a caller.** The
  token carries the Zitadel `sub`, not a `publicId`. service-player owns the
  players table and resolves it locally; commerce needs a lookup it does not
  have yet, so its values stay null in the meantime — an audit trail with a gap
  in exactly the service that needed it.
- Once that lookup exists, commerce **rejects any authenticated write it cannot
  attribute** rather than storing a null, and the column is tightened to
  `NOT NULL`. Availability is the price: a caller commerce cannot resolve gets
  an error even though the purchase itself is well formed.
- A writer with no human actor gets a `SystemActor` constant rather than a null.
  The first is commerce's own event consumer. Every later job adds a constant,
  and nothing in the schema distinguishes those UUIDs from a player's, so they
  are written in a recognisable zero-padded form and read through `SystemActor`.
- The ledger becomes UUIDs throughout. Anything showing `createdBy` to a human
  has to resolve it first, against the players table or against `SystemActor`.
- Lombok has to change with it. `@Builder` ignores inherited fields, so every
  entity moves to `@SuperBuilder`, and `@Data` on a subclass generates an
  `equals` blind to them — `BaseEntity` defines `equals`/`hashCode` on
  `publicId` instead, and the children use plain `@Getter`/`@Setter`.
- Commerce has no `@EnableJpaAuditing` today. It gains a `JpaConfig` like
  service-player's, plus the `AuditorAware` bean in both.
- Every table now needs `public_id` with a `DEFAULT gen_random_uuid()` in its
  migration, so 0003's two-mechanism problem applies everywhere, not just to
  `players` and `characters`.
- Two extra columns and a unique index on every table, including tables nothing
  ever looks up by `publicId`. Paid uniformly to keep the rule simple.

---

*Implemented by:* `refactor: a shared module and one base entity`, and for
`Purchase` by `docs+feat: one canonical player identity across services`
