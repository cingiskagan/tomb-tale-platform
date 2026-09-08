# 10. Integration tests are the default

- **Date:** 2026-07-10
- **Status:** accepted

## Context

The suite was built out of mocked collaborators. A service test mocked its
repository. A repository test mocked the QueryDSL fluent chain. Those tests
asserted that the code calls the methods it calls, so they passed whether or not
the application worked, and they had to be rewritten every time the
implementation moved.

A test that mocks every neighbour cannot fail when real behaviour breaks, and a
test that cannot fail is not evidence of anything.

The tempting move was to defer a testing standard until `service-inventory`, the
first service that would be written from scratch under it. That would have meant
two standards in one repository, and a growing pile of tests nobody trusted in
the meantime.

## Decision

We will make integration tests the default and unit tests the exception, and we
will apply that now, to the services that already exist.

The question to ask of any new test is: **would a plain JUnit test with no mocks
tell me anything?** If yes — pure logic with many input cases — write a unit
test. If no, because the class only coordinates other classes, write no unit
test. The slice tests already cover it.

Three sizes, and nothing in between:

| Tool | What is real | What it tests |
| --- | --- | --- |
| `@WebMvcTest` | Controller, security, validation, JSON, exception handler | 401 and 403, 400 on bad input, response shape |
| `@DataJpaTest` with Testcontainers | Repositories against a real Postgres in Docker | Queries, filters, sorting, schema truth |
| `@SpringBootTest` | Everything | Two or three smoke tests per service, and no more |

## Consequences

- Mocking a repository inside a service test is now a warning sign rather than a
  habit. That assertion belongs in a slice test.
- The classes that earn a unit test can be named, which is the point:
  `ZitadelRoleConverter` (claim parsing), the purchase status state machine,
  `AuthService.getUserProfile()` role parsing, the player-creation retry logic,
  and the game logic still to come — damage formulas, XP curves, dungeon seeds.
- `@WebMvcTest` does not load our own `SecurityConfig`. Without
  `@Import(SecurityConfig.class)` the test exercises Spring's default security
  and passes while the real rules are wrong. This is the failure mode the whole
  approach exists to prevent, and the tooling walks straight into it.
- Test tokens are built with the `jwt()` post-processor from
  `spring-security-test`, with roles in the Zitadel claim rather than in `scope`
  (see [0002](0002-zitadel-is-the-only-identity-provider.md)). An imported
  `SecurityConfig` also needs a `@MockitoBean JwtDecoder` *and*
  `@ActiveProfiles("test")`, because `application.yml` reads `issuer-uri` from an
  environment variable that tests do not set, and the context fails on the
  unresolved placeholder before the mock can help.
- Spring Boot 4 ships Jackson 3 (`tools.jackson`), so there is no
  `com.fasterxml.jackson.databind.ObjectMapper` bean to inject and
  `@Autowired ObjectMapper` fails the context outright. Jackson 2 is present only
  transitively, for YAML and JSR-310. Use `JsonPath`, or the Jackson 3
  `JsonMapper` bean.
- `@DataJpaTest` now requires a running Docker daemon. See
  [0007](0007-testcontainers-instead-of-mocked-repositories.md).
- The frontend follows the same split. `TestBed` component tests are its
  integration tests; plain specs without `TestBed` are its unit tests, for pure
  logic only.
