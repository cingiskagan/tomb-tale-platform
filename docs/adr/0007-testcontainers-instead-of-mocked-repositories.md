# 7. Testcontainers instead of mocked repositories

- **Date:** 2026-08-31
- **Status:** accepted

## Context

The two `*QueryRepositoryImplTest` classes stubbed the QueryDSL fluent chain with
Mockito. They asserted that the code called the methods it calls, which restates
the implementation rather than testing it. One of them asserted on a predicate's
`toString()`. Neither could have caught a wrong predicate, a broken sort, or a
schema that no longer matched the entities.

The suite also ran on H2 with `create-drop`, with Flyway switched off. So the
migrations that build production were never exercised, and the only thing
checking entity-to-schema agreement was a database built from the entities.

## Decision

We will run repository tests as `@DataJpaTest` classes against a real Postgres
17.2 in Docker, started by Testcontainers.

One container per service, shared by the whole suite: a static field on
`PostgresTestBase` annotated `@ServiceConnection`, started in a static
initializer and reaped by Ryuk when the JVM exits.

H2 is removed from both `pom.xml` files, so an in-memory test database cannot be
reintroduced by accident.

## Consequences

- `V1__baseline.sql` builds the schema on every test run and Hibernate validates
  the entities against it. Drift between an entity and a migration now fails the
  build instead of waiting to fail in production.
- Assertions are about observable behaviour. The old player test asserted
  `IllegalArgumentException` for an unknown sort field; through the Spring Data
  proxy, exception translation wraps that in
  `InvalidDataAccessApiUsageException`, so no caller could ever have seen what
  the test claimed.
- Tests require a working Docker daemon. There is no in-memory fallback, and that
  is the intent.
- Runtime is container startup plus the suite: player runs 30 tests in about 25
  seconds, commerce 64 in about 30.
- `./run-local.sh --test` is gone. It existed only to run the app on H2.
- Honest tests surfaced real defects rather than hiding them. Commerce did not
  validate sort fields, so `?sort=nonsense` reached Hibernate as
  `UnknownPathException` and both services answered 500.
  `shouldFailOnUnknownSortField` pinned that behaviour and was named for what it
  was, and the defect was fixed separately in `7ba8630`.

---

*Implemented by:* `8bd4b5f` test: replace mocked repository tests with Testcontainers
