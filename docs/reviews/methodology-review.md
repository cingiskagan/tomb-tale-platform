# Tomb Tale Platform — Methodology Review

**Date:** 2026-07-10
**Scope:** how this repo is built — commit history, test design, config/secrets handling, error handling, dependencies, documentation, CI. Companion to `architecture-review.md` (2026-07-10), which covers *what* was built; this covers *how you work*. Findings there are referenced, not repeated.
**Context:** solo developer, dual goal — ship a platform *and* build interview-grade full-stack skills. Substantial portions produced through Antigravity agents.

---

## 1. Executive Summary

You have strong **process scaffolding** — feature branches and PRs even solo, conventional commits, a pre-PR script mirroring CI, Checkstyle/PMD/MegaLinter/CodeQL/Codecov all wired, secrets genuinely kept out of git. On paper this looks like a disciplined team's repo. That's real and worth keeping.

But underneath the scaffolding, **the two skills that matter most in interviews — testing and verification — are being simulated, not practiced.** Your backend test suite is dominated by mock-verification tests that cannot fail for real reasons (`PlayerQueryRepositoryImplTest.java` mocks the query it claims to test). Your frontend has exactly one spec file for ~20 components, services, and guards — and frontend tests don't run in CI at all. Your 80% coverage gate is met by shrinking the denominator. A senior interviewer opening this repo would see the gap between the polish of the tooling and the hollowness of what it verifies within about ten minutes — and that gap is the single most damaging thing here for the "hold up in interviews" goal.

Third: **the agent-heavy workflow is visible in the artifacts, and it's costing you the learning.** Commits are day-sized batches of unrelated changes; Javadoc contradicts the code it documents; one service's tests declare an anti-pattern to be "the project convention." These are the fingerprints of generated code that was accepted rather than reviewed. The fix isn't to stop using agents — it's to change your role from *accepting* their output to *auditing* it, because auditing is the skill interviews actually test.

---

## 2. What a Senior Engineer Would Flag

### Testing (the biggest cluster)

- **Mocked-fluent-chain repository tests.** `service-player/src/test/.../PlayerQueryRepositoryImplTest.java` mocks `JPAQueryFactory`, `JPAQuery`, and the count query, stubs the entire fluent chain in `setUp()` (lines 50–62, with `Strictness.LENIENT` to silence Mockito's protests), then asserts that `offset()` and `limit()` were called. The actual QueryDSL predicates — the display-name filter, the level-range logic, the sort whitelist — never execute against anything. `PurchaseQueryRepositoryImplTest.java` follows the same pattern. These tests would pass if `findByFilter` returned wrong rows for every filter. In an interview, "walk me through a test that would catch a real bug" has no good answer in this suite.
- **Controllers tested as plain objects.** `PurchaseControllerTest.java:31-33` says it outright: *"Follows the project convention of testing controllers without `@WebMvcTest` — directly instantiating with mocked dependencies."* That "convention" (an agent's rationalization, judging by the phrasing) means nothing tests: `@Valid` enforcement, JSON serialization, path mapping, `GlobalExceptionHandler` behavior, or — critically — security. Test names like `shouldDelegateCreateToService` verify that a one-line method executes its one line.
- **Security is 100% untested where it matters.** The only `@PreAuthorize` in the platform (`CharacterController.java:41`) is bypassed by its own test (`CharacterControllerTest.java` calls the controller directly — the annotation never runs). `spring-security-test` is on both classpaths (`pom.xml` in each service) and is used only by `SecureTestControllerTest.java` — a scaffold endpoint. No test anywhere asserts a 401 or 403 on a real endpoint. Given the authorization holes documented in `architecture-review.md` §M1, a security test pass would have *found* those holes — this is the concrete cost of the gap.
- **One frontend spec.** `frontend-portal/src/app/app.spec.ts` is the entire frontend test suite. Untested: role-claim parsing with real branching logic (`auth.service.ts:94-107`), `roleGuard`, `authGuard`, `authInterceptor`, both API services, and every feature component. Karma/Jasmine is fully configured and runs in `scripts/pre-pr-tests.sh` — the machinery exists, the practice doesn't.
- **Coverage gate is met by exclusion.** Jacoco excludes `config/`, `entity|model/`, `dto/`, `exception/`, `mapper/` (both `pom.xml`s); `codecov.yml` mirrors it — and references `serviceplayer/model/**`, a package that doesn't exist (entities live in `entity/`; the exclusion silently does nothing, and nobody noticed because nothing red happened). Excluding `exception/` means `GlobalExceptionHandler` — real logic, real branching — is both untested and invisible to the 80% target.
- **H2-only test fidelity** — covered in `architecture-review.md` §G8; the methodology point is that Testcontainers is the standard answer and its absence is a screening question in most Java interviews.

### CI

- **The frontend is not in CI.** `.github/workflows/test-and-coverage.yml` matrix covers only `service-player` and `service-commerce`. No workflow runs `ng test`, `ng lint`, or `ng build`. MegaLinter lints TS files statically, but a broken Angular build or failing spec merges cleanly. Your pre-PR script covers it locally — which means the enforcement exists only if you remember to run it, which is exactly what CI is for.
- **CI green ≠ meaningful.** Because of the test-design issues above, the impressive pipeline (CodeQL, MegaLinter with gitleaks/checkov, Codecov with `fail_ci_if_error`) verifies style and secrets but almost no behavior. A senior engineer will notice the inversion: strictest-possible linting, near-zero behavioral verification.

### Commit history

- **Day-sized batch commits of unrelated changes.** Nearly every commit message has the shape "feat: implement X, update Y, and fix Z" — e.g. `a6f0497` ("migrate service-player to JPA/QueryDSL **and** implement player management module in frontend-portal") is a database-layer migration and a new frontend feature in one commit; `ccd6cc9` bundles an entity refactor, CORS security, and a filtering feature. The three-clause message is a symptom: these are agent-session dumps, not atomic changes. `git revert` is useless on this history; `git bisect` nearly so.
- **No commit bodies.** Not one commit in the last 50 explains *why* — including the ones that most need it (`940d1dd` deletes an entire service; `28c54d3` changes "service security and initialization settings" with no rationale). Interviewers read commit history; "what does this repo's history tell me about how you think" is currently answered with "it doesn't."
- **What's good:** conventional-commit discipline is consistent, feature-branch + PR flow (10 PRs) is real, and the `f09d840` → `940d1dd` build-then-delete arc (commerce v1 scrapped in a tech-stack change) is honest iteration, not a flaw.

### Documentation and code hygiene

- **Doc rot that contradicts code** — the most Antigravity-flavored finding. `service-commerce/config/SecurityConfig.java:45-49` Javadoc says "All paths are currently permitted (no authentication required)" while the code enforces JWT auth on `/api/**`. `CreatePurchaseRequest.java:14-17` justifies client-supplied `playerId` "while JWT authentication is disabled" — it's enabled. `README.md` says service-player is "not implemented yet" under Run Backend Services; it's the most developed service in the repo. Each is small; together they say *generated text was committed without being read*, which is the exact habit an interviewer probes when they ask you to explain your own code.
- **Dead code and scaffolding left in place:** `PlayerNotFoundException.java` (never thrown), `TestController`/`SecureTestController` in commerce, unused AMQP config in both services (see `architecture-review.md` §M6).
- **Tooling appeased rather than heeded:** `@SuppressWarnings("PMD.TooManyStaticImports")`, `@MockitoSettings(strictness = LENIENT)`, checkstyle line-length raised when it complained (`277a2ca`). Individually defensible; as a pattern, the linters are being negotiated with instead of listened to.

### What passes senior review cleanly

Secrets handling is genuinely good: `.env`, `.zitadel-masterkey`, PATs all untracked (verified), `.env.example` maintained, ID masking in logs (`LogUtils`), CSRF-state validation in `get-token.sh`, pinned image tags. Dependency choices are sane and current (Spring Boot 4, Angular 20, QueryDSL/MapStruct are defensible picks), and the Jackson CVE TODO in `service-player/pom.xml` — a documented, monitored, can't-fix-yet vulnerability — is exactly the right professional move. The race-condition handling *and its test* (`PlayerServiceTest.shouldRecoverFromConcurrentCreationConflict`) is the single best engineering moment in the test suite: a real concurrency scenario, reasoned about and verified.

---

## 3. Where "It Works" Is Costing You

1. **Coverage theater bought you a green badge and no safety net.** The 80% Codecov target is satisfied, so refactoring *feels* safe — but the tests verify delegation, not behavior. When you restructure commerce (which `architecture-review.md` §M5 says you must), almost nothing will catch regressions. Cost: to the codebase, false confidence; to your learning, you've practiced writing tests without practicing *testing*.
2. **`ddl-auto: update` + backfill-on-login instead of migrations.** `PlayerService.backfillCharacterIfMissing` (`PlayerService.java:93-108`) is schema evolution implemented as request-time application logic — it ships fast and permanently occupies the login path. Every interview asks about migrations; this repo's answer is "Hibernate handles it," which is a red-flag answer. Cost compounds with data (see arch review §M3).
3. **Frontend role guards stood in for backend authorization** (`app.routes.ts:36-44` vs. one `@PreAuthorize` total). It works — the demo shows admins seeing admin menus — and it deferred the actual skill: designing and testing a server-side authorization model. That skill is now blocking, per the architecture review's #1 finding.
4. **Copy-paste was the reuse strategy, and it has already diverged.** The two `SecurityConfig.java` files differ in role mapping, method security, and CORS verbs (arch review §M4). Extracting shared code felt slower than pasting; the divergence is now a live CORS bug waiting for the first commerce PATCH endpoint.
5. **Provisioned-but-unused infrastructure simulated progress.** RabbitMQ, MongoDB, Redis, publisher-confirms config — the platform *looks* event-driven and polyglot. None of it runs. The learning you'd get from one real event flowing end-to-end (serialization decisions, idempotency, failure modes) was deferred while the compose file got longer.
6. **Accepting agent output wholesale optimized for feature-count over understanding.** The evidence is in §2: contradictory Javadoc, a test anti-pattern enshrined as "convention," an exclusion rule pointing at a nonexistent package. Each one shipped a feature faster; each one is a place where, in an interview, you'd be explaining code you've effectively never read.

---

## 4. What You're Not Practicing

Skills full-stack roles screen for, with zero or near-zero evidence in this repo:

- **Integration testing.** No `@WebMvcTest`, no `@DataJpaTest`, no Testcontainers, no `MockMvc` + `spring-security-test` on real endpoints, no single test that exercises HTTP → controller → service → repository → database. This is the most common backend interview screen after algorithms.
- **Database engineering.** No migrations, no hand-written DDL, no index decisions, no query analysis. Everything is delegated to `ddl-auto`. Related latent bug you'd find with one `EXPLAIN` session: `Player.characters` is `FetchType.EAGER` (`Player.java:73`), so `GET /api/v1/players` with pagination is an N+1 factory — and no test or measurement exists to surface it.
- **Frontend testing.** TestBed component tests, guard/interceptor tests with mocked `OAuthService`, HttpTestingController for the API services. Angular roles are screened on exactly this.
- **Observability and debugging under instrumentation.** No structured logging, no correlation IDs, no tracing on your own services (arch review §G6). "How would you debug a slow request across two services" needs a practiced answer, not a theoretical one.
- **Deployment.** No Dockerfiles for your own services, no image build in CI, no environment promotion story (`environment.prod.ts` placeholders reference a pipeline that doesn't exist). "How does your code get to production" is a guaranteed interview question.
- **Decision records.** `docs/design/` contains plans (good ones), but no ADRs. There's no artifact anywhere explaining *why* microservices, *why* Zitadel over Keycloak, *why* QueryDSL — decisions you made and should be able to defend. Writing ADRs is cheap practice for exactly the "justify your architecture" interview conversation.
- **Code review as a skill.** Solo + agents means nothing is ever reviewed. The doc-rot findings show it. You can't practice receiving review here, but you can practice *giving* it — to the agent's diffs, before merge, in writing (PR self-review comments). Your PRs currently merge with no review notes.
- **API design deliberateness.** PUT-that's-actually-PATCH (`PurchaseController.java:105`), a GET with write side effects (`PlayerController.java:48`, arch review §R5), `Page<T>` as a wire contract (§R3) — each suggests endpoint shape is whatever the generator emitted, not a decision you made.

---

## 5. Sequencing From Here

The client's late arrival is an asset: you have a window where breaking changes are free. Use it in this order.

1. **Verification before features.** Every finding in the architecture review (authorization, identity, commerce redesign) requires touching existing behavior. Build the safety net first — real integration tests — or you'll be doing risky surgery with mocked-chain tests as your only witness. This inverts your current instinct (feature first, tests to satisfy the gate).
2. **Security fixes as the first test-writing exercise.** Arch review §M1 (authorization) and this review's testing gap are the same work item: fix the holes *by writing the failing security tests first*, then making them pass. You practice `@WebMvcTest` + `spring-security-test` on code you urgently need to change anyway.
3. **Migrations before any new entity.** Flyway baseline (arch review §M3) before `service-inventory` or the commerce redesign adds tables. Retrofitting migrations onto two services is a day; onto four services with data is a project.
4. **One vertical slice done completely before breadth.** The pathway doc (`docs/design/pathway.md`) queues Phase 2 (inventory) and Phase 3 (dungeons). Resist. Take the existing player/character slice to *actually done* — migrated schema, integration-tested, security-tested, one published event, containerized, frontend specs for its components. That finished slice is worth more in interviews than two more half-services, because it's the thing you can walk through end-to-end under questioning.
5. **Then breadth, with the template.** New services copy the finished slice's patterns (shared security module, Flyway, Testcontainers, event contracts) instead of copying the current drift.
6. **Client-facing contract work last in this window but before the client starts:** OpenAPI export, stable pagination envelope, error-contract unification (arch review §M4/R3/R4) — cheap now, breaking later.

---

## 6. 30-Day Plan

Ordered; each item names the finding it addresses. Assumes part-time solo pace — roughly one item per 2–3 evenings. Do the code for weeks 1–2 by hand; use agents again from week 3, under the review protocol in item 12.

**Week 1 — Security, test-first (§2-security, §3.3, arch §M1)**
1. Write failing `@WebMvcTest` security tests for `service-commerce`: anonymous → 401; authenticated non-admin hitting `PUT/DELETE /api/v1/purchases/{id}` → 403; owner creating a purchase → `playerId` taken from JWT, not body. Use `spring-security-test`'s `jwt()` post-processor with the Zitadel roles claim.
2. Make them pass: port `ZitadelRoleConverter` + `@EnableMethodSecurity` into commerce, derive `playerId` from `jwt.getSubject()`, add `@PreAuthorize` on admin ops, gate `PlayerController.listPlayers`.
3. Same treatment for `CharacterController` — a `@WebMvcTest` proving the existing `@PreAuthorize` actually rejects a plain `player` role (currently nothing verifies it ever ran).

**Week 2 — Real persistence layer (§2-testing, §4-database, arch §M3/G8)**
4. Add Flyway to both services: `V1__baseline.sql` generated from current schema, `ddl-auto: validate`. Split Postgres schemas per service in `infrastructure/init-db.sh` while you're in there.
5. Add Testcontainers (Postgres) and rewrite `PlayerQueryRepositoryImplTest` and `PurchaseQueryRepositoryImplTest` as `@DataJpaTest`-against-Postgres tests that insert rows and assert filter/sort behavior. **Delete the mocked-chain versions** — they're negative-value.
6. While there: fix `Player.characters` to `LAZY` with an explicit fetch strategy for `/me`, and write the repository test that would have caught the N+1 (assert query count or use a projection for the list endpoint).

**Week 3 — Frontend testing exists now (§2-frontend, §2-CI)**
7. Specs for the pure-logic core: `AuthService.getUserProfile()` role-claim parsing (map-form, array-form, garbage-form claims), `roleGuard` allow/deny/redirect, `authInterceptor` attaches-token-only-to-API-URLs. Mock `OAuthService`; no browser gymnastics needed.
8. One component test done properly (`player-list.component` with a stubbed `PlayerService` via `HttpTestingController`) — the template for future components.
9. Add a `frontend` job to `.github/workflows/test-and-coverage.yml`: `npm ci && npx ng lint && npx ng test --watch=false --browsers=ChromeHeadless && npx ng build`. CI now covers all three modules.

**Week 4 — Integration seams + working habits (§3.5, §4-decision-records, §2-commits, arch §M6)**
10. Ship one real event end-to-end: `player.created` published from `PlayerService.createNewPlayerWithCharacter` via `RabbitTemplate`, consumed by a logging consumer in commerce. Write the event schema down in `docs/design/events.md` with a version field. Delete the AMQP config if you decide against this instead — either way, no more dead scaffolding.
11. Write your first three ADRs (template: context/decision/consequences, one page): "microservices vs modular monolith," "canonical player identity" (arch §M2 — make the decision while writing it), "Zitadel." Put them in `docs/adr/`.
12. Adopt the two working-habit rules for everything after this plan: **(a)** atomic commits — one logical change, imperative subject, body says *why* (if the subject needs "and," split it); **(b)** agent-output review protocol — before committing generated code, read the diff line-by-line and leave at least one written self-review comment on the PR; reject any diff whose comments/docs you haven't verified against the code (that's how `SecurityConfig.java:45` happened).

**Deliberately excluded from the 30 days:** inventory service, dungeon service, commerce shop redesign, Dockerfiles, observability. They're next — items 1–12 make them cheaper and make you better at them. If you finish early, Dockerfiles for the two services (§4-deployment) is the highest-value stretch item.
