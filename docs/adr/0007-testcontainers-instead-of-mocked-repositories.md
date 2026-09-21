# 7. Testcontainers instead of mocked repositories

- **Date:** 2026-08-31
- **Status:** accepted

## Context

The two `*QueryRepositoryImplTest` classes stubbed the QueryDSL chain with
Mockito, so they asserted that the code calls the methods it calls. The suite
also ran on H2 with Flyway off, so the real migrations were never exercised.

## Decision

We will run repository tests as `@DataJpaTest` classes against a real Postgres
17.2 in Docker, one container per service on a static `@ServiceConnection` field
in `PostgresTestBase`. H2 leaves both `pom.xml` files.

## Consequences

- `V1__baseline.sql` builds the schema on every run and Hibernate validates the
  entities against it, so drift fails the build instead of production.
- Tests need a working Docker daemon and have no in-memory fallback. That is
  the intent, and `./run-local.sh --test` is gone with H2.
- Honest tests found a real defect. Neither service validated sort fields, so
  `?sort=nonsense` answered 500. `shouldFailOnUnknownSortField` pinned that
  behavior, and `7ba8630` fixed it.

---

*Implemented by:* `8bd4b5f` test: replace mocked repository tests with Testcontainers
