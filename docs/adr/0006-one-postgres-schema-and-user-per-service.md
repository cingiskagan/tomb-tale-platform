# 6. One Postgres schema and user per service

- **Date:** 2026-08-29
- **Status:** accepted

## Context

Both services shared the `public` schema, and `init-db.sh` granted each of them
`ALL PRIVILEGES` on it. Either service could drop the other's tables. A bad
migration in commerce could take player down with it, and nothing in the database
would have stopped it.

Sharing one schema also forced the three Flyway workarounds from
[0005](0005-flyway-owns-the-schema.md): a per-service history table name,
`baseline-on-migrate`, and `baseline-version: 0`. They existed only because each
service was migrating a schema that already contained tables it did not own.

## Decision

We will keep one Postgres instance and give each service its own user and its own
schema, owned by that user. `svc_player` owns `player`; `svc_commerce` owns
`commerce`. Neither has any rights in `public`.

Ownership covers CREATE, ALTER and DROP inside the schema, so no per-table grants
and no `ALTER DEFAULT PRIVILEGES` are needed.

The schema a service targets is set in its `application.yml`, by
`spring.jpa.properties.hibernate.default_schema` and `spring.flyway.schemas`,
where a reviewer can see it. The `ALTER ROLE ... SET search_path` in `init-db.sh`
is a backstop for psql sessions and unqualified SQL, not the authoritative
setting.

## Consequences

- One service cannot reach the other's tables, whatever it does. The isolation is
  enforced by Postgres rather than by convention.
- Flyway now migrates an empty schema it owns, so all three settings from 0005
  are deleted.
- This isolates data, not infrastructure. One instance still means one server to
  lose and one connection limit to exhaust.
- The tables were dropped and rebuilt by `V1` rather than moved with
  `ALTER TABLE ... SET SCHEMA`. Nothing was deployed, and `V1` uses unqualified
  names, so a rebuild leaves one migration describing the schema instead of a
  baseline plus a correction. That shortcut was available exactly once, and it is
  now spent.
- `default_schema` applies to tests as well, so the test profile has to create its
  schema from the H2 JDBC URL, and commerce additionally sets the connection
  schema so the unqualified inserts in `data.sql` resolve. This one did not last:
  [0007](0007-testcontainers-instead-of-mocked-repositories.md) removed H2 from
  both poms two days later. Read this bullet as history, not as current setup.

---

*Implemented by:* `33c41ea` feat(infra): give each service its own Postgres schema
