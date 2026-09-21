# 10. Integration tests are the default

- **Date:** 2026-07-10
- **Status:** accepted

## Context

The suite was built out of mocked collaborators. A service test mocked its
repository, and a repository test mocked the QueryDSL chain, so both passed
whether or not the application worked. A test that cannot fail is not evidence.

## Decision

We will make integration tests the default and unit tests the exception, in the
services that already exist. Ask of any new test: would a plain JUnit test with
no mocks tell me anything? If yes, it is pure logic and earns a unit test. If
no, a slice test covers it. `@WebMvcTest` holds controller, security,
validation and JSON. `@DataJpaTest` on real Postgres holds queries, filters and
schema truth. `@SpringBootTest` holds two or three smoke tests per service.

## Consequences

- Mocking a repository inside a service test is a warning sign now, not a habit.
- `@WebMvcTest` does not load our `SecurityConfig`, so a test can pass against
  Spring's defaults while the real rules are wrong. The import needs a
  `@MockitoBean JwtDecoder` and `@ActiveProfiles("test")`, and its tokens carry
  roles in the Zitadel claim ([0002](0002-zitadel-is-the-only-identity-provider.md)).
- Spring Boot 4 ships Jackson 3, so `@Autowired ObjectMapper` fails the context.
- `@DataJpaTest` needs Docker
  ([0007](0007-testcontainers-instead-of-mocked-repositories.md)).
