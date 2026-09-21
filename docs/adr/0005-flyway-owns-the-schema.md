# 5. Flyway owns the schema

- **Date:** 2026-08-26
- **Status:** accepted

## Context

Hibernate built the schema with `ddl-auto: update`, a mode that only adds. It
left two dead `NOT NULL` columns on `players`, so inserting a player failed, and
`JPA_DDL_AUTO` let any deployment hand the schema back to Hibernate.

## Decision

We will make Flyway the owner. Each service gets a `V1__baseline.sql` dumped
with `pg_dump` from the schema `ddl-auto: update` had already built. Hibernate
drops to hardcoded `ddl-auto: validate`, and `JPA_DDL_AUTO` is deleted.

## Consequences

- An entity change with no migration fails startup instead of altering a table
  quietly, and every entity change now needs a migration written by hand.
- Both services still share `public`, so each needs its own history table,
  `baseline-on-migrate` and `baseline-version: 0`, all three removed by
  [0006](0006-one-postgres-schema-and-user-per-service.md). Tests run on H2
  with Flyway off, so these migrations are never exercised until
  [0007](0007-testcontainers-instead-of-mocked-repositories.md).

---

*Implemented by:* `40b6d18` feat: add Flyway migrations to both services
