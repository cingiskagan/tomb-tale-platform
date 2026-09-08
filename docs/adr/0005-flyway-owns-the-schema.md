# 5. Flyway owns the schema

- **Date:** 2026-08-26
- **Status:** accepted

## Context

Hibernate built the schema through `ddl-auto: update`. That mode only ever adds.
It does not drop a column, does not rename one, and does not write down what it
did. Nobody could say what the schema was except by inspecting a running
database.

It had already left damage behind. `players.level` and `players.experience_points`
were dead columns from a refactor that moved those stats onto `GameCharacter`.
`ddl-auto: update` had added them and would never remove them, and Hibernate's
`validate` does not report columns it has no mapping for, so nothing in the build
would ever have flagged them. Both are `NOT NULL` with no default, which means
inserting a new player against that schema fails.

On top of that, a `JPA_DDL_AUTO` environment variable meant any deployment could
hand schema control back to Hibernate.

## Decision

We will make Flyway the owner of the schema.

Each service gets a `V1__baseline.sql`, generated with `pg_dump` from the schema
`ddl-auto: update` had already built. The first migration is therefore a
photograph of what exists, not a rewrite of it.

Hibernate drops to `ddl-auto: validate`, hardcoded. The `JPA_DDL_AUTO` variable
is deleted, and so is the dev profile's own `update` override.

## Consequences

- The schema is a file that can be read and reviewed. An entity change with no
  matching migration fails startup instead of quietly altering a table.
- No deployment-time setting can give schema control back to Hibernate. Removing
  the variable is the point, not a side effect.
- Every entity change now needs a migration written by hand.
- `defer-datasource-initialization` had to come out of commerce's main profile.
  It exists to let `data.sql` run after Hibernate builds the schema, and it works
  by removing the EntityManagerFactory's dependency on the database
  initializers — Flyway among them. Left in place, Hibernate validates before
  Flyway migrates and fails against an empty schema. The test profile keeps it,
  where H2 and `data.sql` make it correct.
- Both services still share the `public` schema, so each needs its own history
  table, plus `baseline-on-migrate` and `baseline-version: 0`. The explicit
  version is required: the default of 1 would make Flyway skip `V1` and leave the
  tables uncreated. [0006](0006-one-postgres-schema-and-user-per-service.md)
  removes all three.
- Flyway is disabled for tests, which run on H2 with `create-drop` while the
  baseline is Postgres SQL. So the migrations that build production are not
  exercised by the suite.
  [0007](0007-testcontainers-instead-of-mocked-repositories.md) fixes that.

---

*Implemented by:* `40b6d18` feat: add Flyway migrations to both services
