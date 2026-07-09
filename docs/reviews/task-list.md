# Task List — From the Architecture and Methodology Reviews

**How to use this file:** Each group below is one meaningful commit. Check a box to say "I want to do this." The order follows the reviews: security first, then data layer, then contracts, then frontend tests, then events and cleanup.

**Legend:**

- ✋ = write this code by hand (the skill is the point — Claude assists and reviews, but does not write it)
- 🤖 = fine to build with Claude, then review the diff line by line
- References like `M1` or `G5` point to findings in `architecture-review.md`; "method section 2" points to `methodology-review.md`

---

## Testing Method — Decided 2026-07-10

This decision applies **now**, to the existing services. We do not wait for `service-inventory`.

**The rule:** Integration tests are the default. Unit tests are only for logic-heavy code. A test is valuable only if it fails when real behavior breaks — a test that mocks every neighbor cannot do that.

**The three test sizes in Spring (learn these names — they are interview vocabulary):**

| Tool | What is real | What it tests |
|---|---|---|
| `@WebMvcTest` | Controller, security, validation, JSON, exception handler | 401/403, 400 on bad input, response shape |
| `@DataJpaTest` + Testcontainers | Repositories + a real Postgres in Docker | Queries, filters, sorting, schema truth |
| `@SpringBootTest` | Everything | 2–3 smoke tests per service, no more |

**The decision question for every new test:** *"Would a plain JUnit test with no mocks tell me something?"*
- Yes (pure logic, many input cases) → write a unit test.
- No (the class only coordinates other classes) → no unit test; the slice tests above already cover it.

**Code in this repo that earns unit tests:** `ZitadelRoleConverter` (claim parsing), the purchase status state machine (commit A4), `AuthService.getUserProfile()` role parsing, the player-creation retry logic, and all future game logic (damage formulas, XP curves, dungeon seeds).

---

## Phase A — Security (arch M1, method section 2). This phase is also your testing bootcamp.

### Commit A1 — `test(commerce): add security tests for purchase endpoints` ✋

> Learning goal: how Spring builds the web layer, and how to test security for real.

- [ ] Create a `@WebMvcTest` class for `PurchaseController`
- [ ] **Gotcha #1:** `@WebMvcTest` does NOT load your own `SecurityConfig` — without `@Import(SecurityConfig.class)` you test Spring's default security, not yours
- [ ] Test: anonymous request → expect 401
- [ ] Test: authenticated user without admin role calls `PUT/DELETE /api/v1/purchases/{id}` → expect 403
- [ ] Test: when a player creates a purchase, the `playerId` must come from the JWT, not from the request body
- [ ] **Gotcha #2:** build test tokens with the `jwt()` post-processor from `spring-security-test`, and put the roles in the Zitadel claim (`urn:zitadel:iam:org:project:roles`) — not in plain `scope`
- [ ] These tests should FAIL at first — that is the goal of this commit

### Commit A2 — `feat(commerce): enforce roles and JWT identity on purchase endpoints` ✋

- [ ] Add `@EnableMethodSecurity` to commerce `SecurityConfig.java`
- [ ] Wire `ZitadelRoleConverter` into commerce (copy from service-player for now; Phase C will share it)
- [ ] Take `playerId` from `jwt.getSubject()` in the controller; remove it from `CreatePurchaseRequest`
- [ ] Add `@PreAuthorize` to admin operations (update, delete, list-all)
- [ ] All tests from commit A1 now pass

### Commit A3 — `feat(player): restrict player list and verify character authorization` ✋

- [ ] Add `@PreAuthorize("hasAuthority('platform_admin') or hasAuthority('game_master')")` to `PlayerController.listPlayers`
- [ ] Add a `@WebMvcTest` for `CharacterController` that proves the existing `@PreAuthorize` rejects a normal `player` role (nothing verifies this today — the old test calls the controller like a plain object, so the annotation never runs)
- [ ] Delete the old `CharacterControllerTest` — the new slice test replaces it
- [ ] Adopt the rule: every controller method gets an explicit authorization annotation, even if it is only `@PreAuthorize("isAuthenticated()")`

### Commit A4 — `fix(commerce): enforce the purchase status state machine`

> Learning goal: this is the classic case FOR unit tests — a transition table is pure logic.

- [ ] ✋ Write unit tests for the transition table first: every allowed move, every blocked move (e.g. `COMPLETED → PENDING` must fail)
- [ ] 🤖 Block illegal status changes in `PurchaseService.updatePurchase`
- [ ] 🤖 Use the existing `InvalidStatusTransitionException` for all illegal transitions

---

## Phase B — Data Layer and Honest Tests (arch M3/G8, method sections 2 and 4)

### Commit B1 — `feat: add Flyway migrations to both services` ✋

- [ ] Add Flyway dependency to both services
- [ ] Create `V1__baseline.sql` for each service from the current schema
- [ ] Change `ddl-auto` from `update` to `validate`
- [ ] Verify both services start clean against a fresh database

### Commit B2 — `feat(infra): give each service its own Postgres schema` ✋

- [ ] In `infrastructure/init-db.sh`: create a `player` schema and a `commerce` schema
- [ ] Grant each service user privileges ONLY on its own schema (today each can drop the other's tables)
- [ ] Set `hibernate.default_schema` per service
- [ ] Update the Flyway baseline from B1 if needed

### Commit B3 — `test: replace mocked repository tests with Testcontainers` ✋

> Learning goal: your first tests against a real database. This is the most-screened backend interview skill.

- [ ] Add Testcontainers (Postgres) to both services
- [ ] **Gotcha #3:** use ONE shared container for the whole suite (a `static` container with `@ServiceConnection`), not one per test class — otherwise the suite becomes slow and you will hate it
- [ ] Rewrite `PlayerQueryRepositoryImplTest` as a `@DataJpaTest`: insert rows, assert filter and sort results
- [ ] Rewrite `PurchaseQueryRepositoryImplTest` the same way
- [ ] Build test data with helper methods (`aPlayer()`, `aPurchase()`), never from `data.sql`
- [ ] DELETE the old mocked-chain tests — they have negative value (method section 2)
- [ ] REMOVE the H2 dependency from both `pom.xml` files, so nobody (human or agent) can write H2 tests again
- [ ] Speed check: the whole backend suite should stay under ~2 minutes

### Commit B4 — `test: one smoke test per service` 🤖

- [ ] One `@SpringBootTest` per service: the application context starts, and one happy path works end to end
- [ ] Keep it at 2–3 smoke tests per service, forever — everything else belongs in slices

### Commit B5 — `chore: make the coverage gate honest`

- [ ] 🤖 Remove the `exception/**` exclusion from Jacoco in both poms — `GlobalExceptionHandler` is real logic, and the `@WebMvcTest`s now cover it
- [ ] 🤖 Fix the drifted exclusion in service-player: it excludes `model/**`, but entities live in `entity/**` (arch M4)
- [ ] 🤖 Make `codecov.yml` match the Jacoco exclusions exactly, for both services
- [ ] ✋ Decide each remaining exclusion on purpose, and write one line in the commit body about why it stays

### Commit B6 — `fix(player): remove EAGER fetch and the N+1 query on player list`

- [ ] 🤖 Change `Player.characters` to `FetchType.LAZY` (`Player.java:73`)
- [ ] 🤖 Add an explicit fetch strategy (join fetch or projection) for the `/me` endpoint
- [ ] ✋ Write a test that counts queries on the list endpoint, so the N+1 cannot come back

### Commit B7 — `refactor(player): replace login-time backfill with a one-time migration`

- [ ] 🤖 Write a Flyway data migration that gives a default character to players without one
- [ ] 🤖 Delete `backfillCharacterIfMissing` from `PlayerService.java`
- [ ] ✋ Keep the retry-on-collision unit test in `PlayerServiceTest` — that one is real logic and stays

---

## Phase C — Identity and Contracts (arch M2/M4, R3/R4/R5)

### Commit C1 — `docs+feat: one canonical player identity across services` ✋ (the decision) 🤖 (the code)

- [ ] Write an ADR: `Player.publicId` (UUID) is the canonical cross-service player identifier
- [ ] Decide how commerce learns the `publicId` (custom JWT claim via Zitadel action, or a call to service-player)
- [ ] Change `Purchase.playerId` from `String` to `UUID` — do it now, while the only data is five seed rows
- [ ] Update `data.sql` seed rows

### Commit C2 — `refactor: extract shared security and error contract`

- [ ] 🤖 Create a `platform-commons` Maven module: role constants, `ZitadelRoleConverter`, shared error response
- [ ] 🤖 Both services use the same error envelope (today: commerce has `ErrorResponse`, player leaks Spring's default body)
- [ ] 🤖 Delete the dead `PlayerNotFoundException` or start actually using it
- [ ] 🤖 Align CORS between the two services (player allows PATCH, commerce does not — arch M4)

### Commit C3 — `refactor: stable pagination and update semantics`

- [ ] 🤖 Replace raw `Page<T>` in responses with a stable envelope DTO (arch R3)
- [ ] 🤖 Change commerce `PUT /purchases/{id}` to `PATCH` (it already behaves like PATCH)
- [ ] 🤖 Update frontend `common.model.ts` and services to match

### Commit C4 — `refactor(player): explicit profile bootstrap instead of GET side effect` (arch R5)

- [ ] 🤖 Move JIT player creation from `GET /players/me` to an explicit `POST /players/me/bootstrap` (or similar)
- [ ] 🤖 Update `player.resolver.ts` to call the bootstrap once
- [ ] 🤖 Document the required first-call order for future clients

---

## Phase D — Frontend Tests and CI (method section 2)

The same method applies here: `TestBed` component tests are the frontend's integration tests; plain specs without `TestBed` are the unit tests for pure logic.

### Commit D1 — `test(frontend): cover the auth core` ✋

- [ ] Specs for `AuthService.getUserProfile()`: roles claim as map, as array, and as garbage input (pure logic → unit style)
- [ ] Specs for `roleGuard`: allow, deny, and redirect cases
- [ ] Specs for `authInterceptor`: token attached only to API URLs
- [ ] Mock `OAuthService` — no browser tricks needed

### Commit D2 — `test(frontend): first real component test` ✋

- [ ] Component test for `player-list.component` with `TestBed` and `HttpTestingController` (integration style)
- [ ] Use it as the template for all future component tests

### Commit D3 — `ci: add the frontend to GitHub Actions` 🤖

- [ ] New job in `test-and-coverage.yml`: `npm ci`, `ng lint`, `ng test --watch=false --browsers=ChromeHeadless`, `ng build`
- [ ] After this commit, a broken frontend can no longer merge silently

---

## Phase E — Events, Routing, Cleanup (arch M6/M7, method section 2)

### Commit E1 — `feat: first real event — player.created`

- [ ] ✋ Design the event schema with a version field; write it in `docs/design/events.md`
- [ ] 🤖 Publish `player.created` from `PlayerService.createNewPlayerWithCharacter` via `RabbitTemplate`
- [ ] 🤖 Add a simple consumer in commerce (logging is enough for now)
- [ ] Alternative: if you decide events are too early, remove ALL AMQP config instead — no more dead scaffolding

### Commit E2 — `fix(infra): safe Traefik routing` (arch M7)

- [ ] 🤖 Make platform API routes impossible to fall through to Zitadel (today a forgotten prefix silently routes to the identity provider)
- [ ] 🤖 Add a catch-all 404 router between the service routes and Zitadel's `/api` alias
- [ ] 🤖 Document the port/prefix registry in the infrastructure README

### Commit E3 — `chore: remove dead code and fix lying documentation` 🤖

- [ ] Delete `TestController` and `SecureTestController` from commerce
- [ ] Fix the Javadoc in commerce `SecurityConfig.java:45-49` (it says all paths are open; they are not)
- [ ] Fix the Javadoc in `CreatePurchaseRequest.java` (it says JWT auth is disabled; it is enabled)
- [ ] Fix `README.md` (service-player is not "not implemented yet")
- [ ] Review each fix yourself: this commit is practice for auditing generated text

### Commit E4 — `docs: first architecture decision records` ✋

- [ ] ADR: microservices vs modular monolith (defend the choice you made)
- [ ] ADR: why Zitadel
- [ ] ADR: testing strategy — write the "Testing Method" section above in your own words; if you cannot, you have not learned it yet
- [ ] ADR template: context / decision / consequences, one page each, in `docs/adr/`

---

## Backlog — After These Phases (not yet commits)

- [ ] Commerce shop redesign: item catalog, wallet, idempotent purchase endpoint (arch M5, G1, G5)
- [ ] Dockerfiles + image builds in CI for all three modules (arch G7)
- [ ] Machine-to-machine auth for the future game server (arch G2)
- [ ] OpenAPI spec export + generated Angular client (arch G4)
- [ ] Observability: structured logs, correlation IDs, tracing (arch G6)
- [ ] Rate limiting at the Traefik edge (arch G9)
- [ ] Account lifecycle: delete, ban, suspend (arch G10)

---

## Working Rules (apply to every commit above)

- One commit = one logical change. If the message needs the word "and", split it.
- Every commit message gets a body that explains **why**.
- Before committing generated code: read the whole diff, check that comments match the code, leave one self-review note on the PR.

**Definition of done for any new endpoint (from now on):**

1. A `@WebMvcTest` covers: 401/403, validation errors, and the happy path's JSON shape.
2. A `@DataJpaTest` covers any new query logic.
3. A unit test exists ONLY if you added pure logic.
4. Warning sign: if you are mocking a repository inside a service test — stop. That assertion belongs in a slice test.
