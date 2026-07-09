# Tomb Tale Platform — Architecture Review

**Date:** 2026-07-10
**Scope:** full repository — `service-player`, `service-commerce`, `frontend-portal`, `infrastructure`, `docs/design`, CI configuration.
**Context:** platform-first development for an online RPG with procedural dungeons (see `docs/design/pathway.md`). Game client (Unity) not started. Parts of the codebase were produced by mixed AI agents (Antigravity), and it shows — several findings below are drift artifacts between documentation, configuration, and code.

---

## 1. Executive Summary

1. **Your authorization model is a UI decoration, not a security boundary.** There is exactly one `@PreAuthorize` annotation in the entire backend (`CharacterController.java:41`). Every commerce endpoint lets any authenticated player create, modify, and cancel *anyone's* purchases — at *prices the client supplies*. The Angular `roleGuard` only hides menu items. Anything you build on top of this API contract inherits the problem, and the game client will be built on top of this API contract.

2. **You have three incompatible notions of "who a player is."** `service-player` keys players by `publicId` (UUID) externally and `zitadelUserId` (JWT sub) internally. `service-commerce` stores `playerId` as a free-text string that the client fills in (`CreatePurchaseRequest.java:26`). There is no shared identity contract, and every future service (`service-inventory`, `service-dungeon` per the pathway doc) will copy one of these patterns at random. This is the single most expensive thing to fix later, because fixing it means migrating ledger data.

3. **The platform's stated architecture doesn't exist yet — and the scaffolding pretends it does.** The TDD (`docs/design/tier-1-technical-design-document.md` §3.4) requires event-driven match reporting through RabbitMQ. RabbitMQ is provisioned, both services declare AMQP dependencies, `service-player` declares an exchange (`RabbitMQConfig.java`) — and **zero** lines of code publish or consume a message. MongoDB is provisioned and unused. This is carrying cost and false confidence: the hard part (event contracts, idempotent consumers) hasn't been started.

---

## 2. Structural Mistakes, Ranked by Cost of Delay

### M1 — No server-side authorization or ownership model (highest cost of delay)

**What it is.**
- `PurchaseController.java` — all five endpoints (create/read/list/update/delete) have no role checks and no ownership checks. Any player with a valid JWT can list all purchases, cancel other players' purchases, or "update" a `COMPLETED` purchase back to `PENDING`.
- `CreatePurchaseRequest.java:26,40` — `playerId` **and** `unitPrice` are client-supplied. The server computes `totalPrice` from a price the attacker chose (`PurchaseService.java:52`). The DTO's own Javadoc admits it: *"The playerId is accepted in the request body while JWT authentication is disabled"* — except JWT auth is **enabled** (`SecurityConfig.java:63`), so this is a stale workaround that became a hole.
- `PlayerController.java:88-94` — `GET /api/v1/players` (full player enumeration with filtering) has no role restriction; any player can dump the player list. The frontend gates the `/players` route to admin/GM (`app.routes.ts:40-45`), which is cosmetic.
- `service-commerce/config/SecurityConfig.java` never wires `ZitadelRoleConverter`, so even if you added `@PreAuthorize("hasAuthority('platform_admin')")` to commerce today, it would silently deny everyone — the Zitadel roles claim is never mapped to authorities in that service. Also missing `@EnableMethodSecurity`, so the annotation wouldn't even be evaluated.

**Why it's wrong.** Authorization enforced only in a SPA is not authorization. For a commerce ledger in a game with a real economy, this is the class of bug that becomes an economy-destroying exploit the week the client ships.

**Cost now:** ~2–4 days. Copy `ZitadelRoleConverter` + `@EnableMethodSecurity` into commerce, derive `playerId` from `jwt.getSubject()`, add `@PreAuthorize` to admin operations, add ownership predicates to player-facing reads.
**Cost in six months:** every endpoint added between now and then repeats the pattern; the client SDK gets built against a contract where the client sends `playerId` and `unitPrice`, and changing it breaks the client; plus whatever an exploit costs you.

**Remediation.**
1. In commerce: add `@EnableMethodSecurity`, wire the role converter (extract it to a shared module — see M4), extract `playerId` from the JWT, delete `unitPrice` from `CreatePurchaseRequest` (price comes from a catalog — see Gap G1).
2. In player: `@PreAuthorize("hasAuthority('platform_admin') or hasAuthority('game_master')")` on `listPlayers`.
3. Adopt a rule: *every* controller method carries an explicit authorization annotation, even if it's a marker like `@PreAuthorize("isAuthenticated()")`, so absence is grep-detectable.

### M2 — Fragmented player identity

**What it is.** Three identity representations:
- `Player.publicId` (UUID, API-facing) — `service-player/entity/Player.java:54`
- `Player.zitadelUserId` (JWT `sub`, used by `/me` endpoints) — `Player.java:58`
- `Purchase.playerId` (free string, client-supplied; seed data uses `"player-001"`) — `service-commerce/entity/Purchase.java:55`, `data.sql`

The frontend juggles the first two already (`player.service.ts` uses `publicId` for admin ops, `/me` for self-ops). Commerce is compatible with neither — you cannot today join a purchase to a player record with any integrity guarantee.

**Why it's wrong.** The pathway doc plans two more services (`service-inventory`, `service-dungeon`) that both need player references. Whatever convention exists when they're written is the one they'll copy. Purchase rows accumulate; identity migration cost grows linearly with ledger size and number of consuming services.

**Cost now:** ~1 day. Decide the canonical cross-service player identifier and document it.
**Cost in six months:** a data migration across the purchase ledger, plus coordinated changes in 3–4 services and the client.

**Remediation.** Make `Player.publicId` (UUID) the canonical cross-service identifier. Commerce derives it by resolving the JWT `sub` → `publicId` once (either by calling `service-player` or, better, by putting `publicId` into the token as a custom claim via a Zitadel action). Change `Purchase.playerId` to `UUID` now, while the only data is five seed rows in `data.sql`. Write the decision into `docs/design/` as an ADR.

### M3 — No schema migrations, and a shared schema that nullifies your service isolation

**What it is.**
- Both services run `ddl-auto: update` (`application.yml` in both; `JPA_DDL_AUTO` defaults to `update`). No Flyway/Liquibase anywhere.
- `infrastructure/init-db.sh` grants **both** service users `ALL PRIVILEGES` on the **same** `public` schema of the **same** database, including default privileges on all future tables. `service_player` can `DROP TABLE purchases;`. The script's comment says "principle of least privilege" — it isn't.

**Why it's wrong.** `ddl-auto: update` never removes or renames anything — it accretes. The `Player.characters` backfill logic (`PlayerService.java:93-108`, "self-healing migration") is already the shape of pain you get without migrations: schema evolution logic living in request-handling code paths, running per-login, forever. And the shared schema means your microservice DB isolation is organizational fiction — one service's Hibernate `update` can collide with the other's tables.

**Cost now:** ~1–2 days. Flyway baseline per service + separate schemas. There is effectively no production data to migrate.
**Cost in six months:** reconstructing a migration history from a drifted `update`-managed schema, with real player data on the line, while two more services have joined the same shared schema.

**Remediation.** Add Flyway to both services (baseline `V1__init.sql` generated from current schema). Set `ddl-auto: validate`. In `init-db.sh`, give each service its own schema (`player`, `commerce`) with privileges only on its own, and set `spring.jpa.properties.hibernate.default_schema` per service. Delete `backfillCharacterIfMissing` and replace with a one-time data migration.

### M4 — Cross-cutting contracts are duplicated by hand, and they're already drifting

**What it is.**
- Security config is copy-pasted between services and has *already diverged*: player wires `ZitadelRoleConverter` + `@EnableMethodSecurity`, commerce has neither; player allows `PATCH` in CORS (`SecurityConfig.java:90`), commerce doesn't (`SecurityConfig.java:87`) — a commerce PATCH endpoint would mysteriously fail from the browser.
- Roles exist as string literals in Java (`CharacterController.java:41`) and as a hand-maintained enum in TypeScript (`auth.models.ts`).
- Error contracts differ per service: commerce returns structured `ErrorResponse` via `GlobalExceptionHandler`; player throws `ResponseStatusException` (`PlayerService.java:145,149`) producing Spring's default error body. A client needs two error parsers for one platform. `PlayerNotFoundException` exists but is dead code — never thrown.
- Frontend DTOs (`core/api/*.models.ts`) are hand-mirrored from Java DTOs with no generation, despite springdoc already being on the classpath in both services.
- Update semantics differ: player uses `PATCH` for partial updates; commerce uses `PUT` and documents it as "partial update — only non-null fields are applied" (`PurchaseController.java:105-106`), which is a PATCH wearing a PUT's clothes.
- Jacoco exclusions drifted: commerce excludes `**/entity/**`, player excludes `**/model/**` — but player's entities live in `entity/`, so they count against player's coverage. Nobody decided that; two agents guessed differently.

**Why it's wrong.** This is the Antigravity-era inconsistency you warned about, and it compounds: each new service copies one of the diverging templates. Every contract that exists twice will disagree eventually; several already do.

**Cost now:** ~2–3 days. Extract a `platform-commons` Maven module (role constants, `ZitadelRoleConverter`, error contract, shared security config baseline); pick PATCH-with-explicit-semantics; publish OpenAPI specs as build artifacts and generate the Angular client.
**Cost in six months:** 4 services × N endpoints of drift; a game client hand-coded against inconsistent contracts.

**Remediation.** As above. If you resist a shared library (defensible for true microservice independence), then the alternative is contract tests — but pick one; right now you have neither.

### M5 — Commerce's domain model is an admin CRUD table, not a commerce system

**What it is.** `service-commerce` is described as "purchases with ACID guarantees" (`pom.xml`) and per `pathway.md` should own "shop storefront, virtual currency wallets, purchase transactions." What exists is a generic CRUD over a `purchases` table where the caller invents the item code and price. There is no item catalog, no wallet/balance, no debit operation, no idempotency key. `PurchaseStatus` documents transitions (`PENDING → COMPLETED → REFUNDED`) but `updatePurchase` (`PurchaseService.java:100-118`) enforces none of them — any state can jump to any state except `CANCELLED`.

**Why it's wrong.** This isn't missing polish; it's a missing domain. The current API shape (client-priced, client-attributed, freely-mutable purchases) cannot evolve into a shop — it must be replaced. The Angular purchase form (`features/purchases/purchase-form.component.ts`) is already built against the throwaway shape, and a game client would be too.

**Cost now:** cheap to acknowledge — treat the current endpoints as admin/back-office tooling, rename or re-scope, and don't build player-facing flows on them.
**Cost in six months:** the client's shop UI, the frontend, and any game-server integration all bind to an API you have to break.

**Remediation.** Split the contract explicitly: `/api/v1/admin/purchases` (current CRUD, role-gated, kept for the portal) vs. a future `/api/v1/shop` (catalog-priced, wallet-debiting, idempotent `POST` with client-generated idempotency key). Enforce the status state machine in `updatePurchase` — `InvalidStatusTransitionException` already exists; use it for all illegal transitions, not just CANCELLED-via-PUT.

### M6 — Messaging and MongoDB are cargo-cult infrastructure

**What it is.** RabbitMQ: provisioned in compose, AMQP starters in both poms, publisher-confirms configured in `service-commerce/application.yml:38-39`, an exchange declared in `service-player/config/RabbitMQConfig.java` — and no `RabbitTemplate`, no `@RabbitListener`, anywhere. MongoDB: provisioned in compose, used by nothing (planned for `service-inventory`/`service-dungeon`, which don't exist). Redis: provisioned, disabled by default, only for Zitadel caching.

**Why it's wrong.** Mostly carrying cost — but publisher-confirms config with no publisher, and an exchange with no messages, mislead every reader (human or agent) into believing an event architecture exists. The TDD's `InstanceResultEvent` flow (§3.4) is the actual integration seam for the game server, and none of the hard decisions (event schema, versioning, idempotent consumption, outbox vs. direct publish) have been made.

**Cost now:** an afternoon to either delete the dead config or publish one real event end-to-end.
**Cost in six months:** minor, but the design decisions blocking the game server don't age well undone.

**Remediation.** Pick one: (a) strip AMQP from both services and the compose file until Phase 2, or (b) implement one real event now — `player.created` published from `PlayerService.createNewPlayerWithCharacter` with a versioned, documented schema — to force the contract decisions while they're cheap. (b) is worth more.

### M7 — Gateway routing is a hand-maintained path list that collides with Zitadel

**What it is.** `infrastructure/traefik-dynamic.yml` routes exactly two path prefixes (`/api/v1/players`, `/api/v1/purchases`, priority 300) to host ports 8081/8082. Meanwhile the compose file gives Zitadel a `PathPrefix(/api)` router at priority 200 that strips `/api` and forwards to Zitadel (`docker-compose.yml`, `zitadel-api-alias-web`). Any new controller prefix you forget to add — `/api/v1/characters`, `/api/v1/shop` — silently routes to **Zitadel's API** instead of returning 404.

**Why it's wrong.** Silent misrouting to your identity provider is a nasty failure mode, and the per-prefix list means gateway config changes for every new controller. Also, the same origin (`localhost:8080`) serves both the IdP and the game APIs in dev, but `environment.prod.ts` assumes split origins (`auth.tombtale.com` / `api.tombtale.com`) — the dev and prod topologies diverge and only dev exists.

**Cost now:** minutes-to-hours. Route `/api/v1/*` (or a `/platform` prefix) to services with a rule that can't fall through to Zitadel; document the port/prefix registry.
**Cost in six months:** a debugging session per forgotten prefix, and a surprise re-plumbing when prod topology is first built.

**Remediation.** Give platform APIs a reserved prefix that Zitadel's routers can never match, or lower/remove Zitadel's generic `/api` alias router (it exists only to alias Zitadel's own API). Add a catch-all 404 router between priorities 300 and 200.

---

## 3. Service Boundaries

**player ↔ commerce is drawn in a defensible place, but the seam is unbuilt.** Player identity/progression vs. economy is a reasonable split. What's missing is the connective tissue: commerce holds player references with no contract (M2), and there is no sync or async communication between the services at all — no REST client, no events. Right now they're two monoliths that happen to share a repo, a database schema (M3), and a copy-pasted security config (M4). That's not two services; it's one distributed system's worth of operational cost for zero distributed-system benefit.

**Too granular, given what exists.** With this feature surface (one aggregate per service, CRUD each), a modular monolith would deliver the same admin portal at half the operational cost. The split is only justified by the roadmap (`pathway.md` plans 4 services with heterogeneous storage). That's a defensible bet — but if you keep it, make the boundaries real: separate DB schemas, an explicit identity contract, and one working cross-service event. Otherwise you're paying microservice tax while accruing monolith coupling.

**Character ownership sits correctly in service-player** (`CharacterService.updateCharacterStats` validates character-belongs-to-player via `CharacterRepository.findByPublicIdAndPlayerPublicId`) — this is the right shape; extend it.

**A boundary that will be drawn soon and is worth pre-deciding:** the TDD's matchmaking/instance orchestration (§3.3) and the dedicated game server both need *read* access to player stats. Decide now whether they call `service-player` REST with a machine token or consume a projected read model via events — because that decision dictates whether `service-player`'s API needs machine-to-machine auth (it currently has no story for non-browser callers; see R2).

**Frontend boundary is clean.** `core/api` vs `core/auth` vs `features` is well-drawn; the resolver-based JIT provisioning trigger (`player.resolver.ts`) is centralized properly. No complaints about where lines sit in `frontend-portal`.

---

## 4. Gaps — What a Platform Like This Normally Has Before a Client Attaches

- **G1 — Item catalog / price authority.** Nothing owns "what items exist and what they cost." Commerce accepts client prices (M1/M5); the pathway's template-vs-instance item model (`pathway.md`) is unstarted. The client cannot render a shop, and the server cannot price a purchase. This is the biggest functional gap.
- **G2 — Machine-to-machine auth.** Everything assumes a browser doing PKCE (`auth.config.ts`, `get-token.sh`). The dedicated game server and matchmaker (TDD §3.2–3.3) need service-account JWTs (Zitadel client-credentials) and the resource servers need to accept them (audience/scope validation — currently neither service validates `aud` at all).
- **G3 — Schema migrations** (M3). Non-negotiable before real player data exists.
- **G4 — API contract artifacts.** springdoc is on both classpaths but specs aren't exported, versioned, or used for client generation. The game client team (future you) needs a frozen, versioned OpenAPI spec per service.
- **G5 — Idempotency.** `POST /api/v1/purchases` has no idempotency key. Game clients retry on flaky mobile networks; every retry is a duplicate ledger row.
- **G6 — Observability for your own services.** The OTEL collector in compose serves only Zitadel (behind a profile). The Spring services expose actuator but no tracing, no correlation IDs across the Traefik hop, no structured request logging. First cross-service bug will be diagnosed by vibes.
- **G7 — Deployability of the services themselves.** No Dockerfiles for `service-player`/`service-commerce`/`frontend-portal`; they run only via `run-local.sh` on the host through `host.docker.internal`. There is no path from this repo to any non-laptop environment, and `environment.prod.ts` placeholders (`REPLACE_IN_CI`) reference a CI replacement pipeline that doesn't exist.
- **G8 — Integration tests against real dependencies.** All tests run on H2, but prod is Postgres with Postgres-specific bits (`ColumnDefault("gen_random_uuid()")` in `Player.java:53`, QueryDSL predicates in `PlayerQueryRepositoryImpl`/`PurchaseQueryRepositoryImpl`). Testcontainers on the repository layer would close the fidelity gap cheaply.
- **G9 — Rate limiting / abuse controls** at the Traefik edge. Trivial to add as middleware now, painful to retrofit under attack.
- **G10 — Account lifecycle.** No deletion, ban/suspend, or Zitadel-webhook story. A `player` row is created JIT on first `/me` call and lives forever. Fine today; decide before public exposure.

Explicitly *not* gaps yet (correctly deferred per your own tiering): matchmaking service, dungeon generation, inventory service, real-time networking.

---

## 5. Client Integration Risks

- **R1 — The auth flow shape is SPA-only.** A Unity client can't do redirect-based PKCE the way `angular-oauth2-oidc` does. Zitadel supports native-app PKCE (loopback/custom-scheme redirect) and device flow, but nothing in the platform anticipates it: token acquisition, refresh, and the proprietary roles claim (`urn:zitadel:iam:org:project:roles`, parsed in *two* hand-rolled places — `auth.service.ts:95` and `ZitadelRoleConverter.java`) will need a third implementation in the client. Extract and document the token contract (required claims, role format, expected audience) now.
- **R2 — Two ID spaces in one API.** Self-operations key on JWT sub (`/players/me`), admin operations key on `publicId` (`/players/{publicId}/characters/...`). A game client acting *as the player* has no way to learn its own `publicId` except by parsing the `/me` response first. Fine — but undocumented, and commerce's third ID form (M2) breaks it entirely.
- **R3 — Spring `Page<T>` as a wire format.** `PlayerController.listPlayers` and `PurchaseController.listPurchases` serialize `PageImpl` directly. Spring explicitly warns its JSON shape is unstable across versions (and Spring Boot 4 is young — you already carry a Jackson-pin TODO in `service-player/pom.xml`). The frontend hand-mirrors it in `common.model.ts`. Switch to Spring Data's `PagedModel` or a stable envelope DTO before a client codegen freezes the accidental shape.
- **R4 — Inconsistent error contract** (M4). A client SDK needs one error envelope. Right now: two.
- **R5 — GET with write side effects.** JIT provisioning fires on `GET /api/v1/players/me` (`PlayerController.java:48-56`, `PlayerService.getOrCreatePlayer`). Any client, cache, or health probe issuing a GET creates DB rows; a game client that calls any *other* endpoint first (e.g. the future shop) gets 404s until it has performed the magic GET. Move provisioning to an explicit `POST /players/me` bootstrap call or an event triggered by Zitadel, and document the required first-call ordering.
- **R6 — Latency-blind CRUD contract.** Every interaction is request/response through Traefik to per-entity endpoints. Acceptable for the portal; wrong for a client that needs "load my profile + characters + wallet + entitlements" at login. Plan an aggregate bootstrap endpoint (or BFF) rather than letting the client fan out N REST calls — especially since the services can't yet talk to each other to compose anything (§3).
- **R7 — CORS/gateway assumptions bake in browser-ness.** Tokens are only attached for URLs matching `environment.apiBaseUrl` (`auth.interceptor.ts:16`), CORS lists exact headers/methods (and already drifted between services, M4). None of this applies to a game client, which is good — but it means today's only tested path (browser → Traefik → service) exercises none of the client's future path (native → Traefik → service, no CORS, machine tokens). G8's integration tests should include a non-browser caller.

---

## 6. What's Actually Fine — Don't Touch

- **The infrastructure compose stack** (`infrastructure/docker-compose.yml`): pinned image tags, real healthchecks, dependency ordering, secrets kept out of git (verified — `.env`, `.zitadel-masterkey`, PATs are all untracked), Zitadel v2 login properly fronted by Traefik. This is better than most hobby-platform infra.
- **`publicId` UUID pattern** for external identifiers (`Player.java:51-54`) — correct instinct; make it universal (M2) rather than changing it.
- **Money handling in commerce**: `BigDecimal` with explicit precision/scale, server-computed `totalPrice`, optimistic locking via `@Version`, soft-delete for ledger audit (`Purchase.java`). The domain is too thin (M5) but these primitives are right.
- **Backend layering** (controller/service/repository/mapper with MapStruct + QueryDSL filter fragments) — consistent between the two services, easy to extend. `getOrCreatePlayer`'s race handling with `DataIntegrityViolationException` catch (`PlayerService.java:70-83`) is genuinely careful.
- **CI and quality gates**: shared Checkstyle/PMD configs, Jacoco + Codecov with per-service flags, MegaLinter with gitleaks/checkov, and `scripts/pre-pr-tests.sh` mirroring CI locally. Keep it.
- **Frontend architecture**: standalone components, functional guards/interceptors, lazy-loaded features, route-data role gating, the profile resolver. The structure is right; only the backend it talks to is lying to it about security (M1).
- **The design docs themselves** (`docs/design/`): the tiering discipline (explicitly deferring trading/crafting, scoping Tier 1 to one room) is the most valuable risk-management artifact in the repo. The gap is that the code hasn't caught up to the TDD's integration requirements — not that the plan is wrong.

---

## Suggested Order of Attack

1. M1 (authorization + ownership) — days, and everything else builds on it.
2. M2 (canonical player ID) + M3 (Flyway + split schemas) — while data is still seed rows.
3. M4 (extract shared security/error/role contracts; export OpenAPI) — before endpoint count grows.
4. M5/G1 (re-scope commerce as admin tooling; design catalog + wallet) — before any player-facing shop work.
5. M6 (one real event end-to-end) + G2 (machine-token auth) — these unblock the matchmaker/game-server design.
6. M7, G5–G9 as they become load-bearing.
