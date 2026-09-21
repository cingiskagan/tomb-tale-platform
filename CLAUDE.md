# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Monorepo for the "Tomb Tale Online RPG" platform: two Spring Boot microservices, an Angular web portal, and Docker Compose infrastructure to run everything locally.

- `frontend-portal/` — Angular 20 portal (standalone components, PrimeNG, OIDC auth via Zitadel)
- `platform-commons/` — the Java library both services depend on: `BaseEntity`, `SystemActor`
- `service-player/` — Spring Boot service for player accounts and characters (port 8081)
- `service-commerce/` — Spring Boot service for purchases/economy (port 8082)
- `infrastructure/` — Docker Compose stack: Traefik, Zitadel (auth), Postgres, Redis, MongoDB, RabbitMQ, Mailpit (catches Zitadel's mail in dev, inbox at :8025)
- `config/checkstyle/`, `config/pmd/` — static-analysis rulesets shared by both Java services
- `scripts/pre-pr-tests.sh` — full local pre-PR check pipeline across all three modules

Both services use Java 21 and Spring Boot 4. The frontend requires the Node version pinned in `frontend-portal/.nvmrc`.

The root `pom.xml` is an aggregator over `platform-commons` and the two services. It is not their parent — each keeps `spring-boot-starter-parent` — and exists only so Maven resolves `platform-commons` from the reactor. Build Java from the repository root.

## Common Commands

### Infrastructure (start first)

```bash
cd infrastructure
docker compose up -d
```

All service ports, credentials, and Zitadel settings come from `infrastructure/.env` (copy from `.env.example`).

### Backend services (service-player, service-commerce)

```bash
cd service-player            # or service-commerce
./run-local.sh               # full mode: real Postgres + RabbitMQ from Docker
```

Maven, from the repository root. `-pl` picks the module and `-am` builds what it depends on, which is how `platform-commons` gets onto the classpath:

```bash
./mvnw -pl service-player -am clean test                                  # unit tests
./mvnw -pl service-player -am test -Dtest=CharacterServiceTest            # single test class
./mvnw -pl service-player -am test -Dtest=CharacterServiceTest#method     # single test method
./mvnw -pl service-player -am verify -DskipTests                          # style/static analysis only
./mvnw -pl service-player -am clean verify                                # tests + Jacoco + checkstyle + PMD (what CI runs)
./mvnw clean verify                                                       # every module at once
```

`cd service-player && ./mvnw clean verify` still works, but only once `platform-commons` is in the local repository — run `./mvnw install -DskipTests` from the root after a fresh clone, or whenever `platform-commons` changes. Dropping `-am` on a clean machine fails with `Could not find artifact com.tombtale:platform-commons`.

Checkstyle/PMD rulesets live in `config/checkstyle/checkstyle.xml` and `config/pmd/pmd-ruleset.xml`, referenced relatively from each `pom.xml` — one copy governs all three modules.

Both `check` goals are bound to the `verify` phase, so `./mvnw clean verify` is the single gate: the same engine at the same pinned version runs locally and in CI. MegaLinter does not lint Java — it ships its own PMD build, and when that drifted from `${pmd.version}` the two disagreed about the same ruleset. Style failures surface after the tests as a result; `./mvnw -pl <module> -am verify -DskipTests` is still the fast path while you are working. Naming the two goals directly — `checkstyle:check pmd:check` — no longer works from a clean tree: a goal invoked without a lifecycle phase never builds `platform-commons`, so resolving the module's dependencies fails before either check runs. `verify -DskipTests` reaches the same two goals, since both are bound to that phase.

### Frontend (frontend-portal)

```bash
npm start                    # ng serve → http://localhost:4200
npm run build
npm test                     # Karma/Jasmine
npx ng test --watch=false --browsers=ChromeHeadless     # headless run (CHROME_BIN must point to a Chrome/Chromium binary)
npm run lint                 # angular-eslint
```

### Full pre-PR check (mirrors CI)

```bash
./scripts/pre-pr-tests.sh                              # everything
./scripts/pre-pr-tests.sh --scope general              # shellcheck, markdownlint, yamllint only
./scripts/pre-pr-tests.sh --scope service-player       # one module only
./scripts/pre-pr-tests.sh --clean                      # also runs npm ci
```

Run this before opening a PR — CI (`test-and-coverage.yml`, `mega-linter.yml`) enforces the same checks, plus Codecov coverage upload. Java style and static analysis belong to the Maven build; MegaLinter covers shell, YAML, Markdown, Dockerfiles, JSON, TypeScript, secrets, and IaC.

## Architecture

### Backend services

Both services follow an identical layering — copy the sibling service's pattern when adding anything new:

```text
controller/  → REST endpoints under /api/v1/..., @PreAuthorize role checks
service/     → business logic, transactional boundaries
repository/  → Spring Data JPA; dynamic filtering via QueryDSL (…QueryRepository + …QueryRepositoryImpl)
entity/      → JPA entities; the QueryDSL Q-classes are generated into this package too
domain/      → plain domain logic with no JPA (service-commerce: PurchaseStatus + its transition table)
dto/         → request/response DTOs with jakarta validation
mapper/      → MapStruct compile-time mappers between entities and DTOs
exception/   → domain exceptions; service-commerce has a GlobalExceptionHandler
security/    → ZitadelRoleConverter — claim parsing, kept out of config/ on purpose
config/      → SecurityConfig, QueryDslConfig, RabbitMQConfig — wiring only, no logic
```

Every entity extends `BaseEntity` from `platform-commons`, which carries the same six fields everywhere: `Long id` (internal, never leaves the persistence layer), `UUID publicId` (the only identifier in an API), `createdAt`/`updatedAt`, and `createdBy`/`updatedBy`. Controllers and DTOs deal only in `publicId`. See ADR 0013.

`publicId` is assigned where the field is declared, not in a `@PrePersist`, so it exists before the first save and equality can depend on it — `BaseEntity` defines `equals`/`hashCode` on it, and entities must not generate their own. Lombok follows from that: `@SuperBuilder` instead of `@Builder`, explicit `@Getter`/`@Setter` instead of `@Data`.

The actor columns come from Spring Data auditing. Each service supplies an `AuditorAware<UUID>` in its `security/` package and wires it in `config/JpaConfig`. service-player resolves the JWT subject to a `publicId` against its own table; service-commerce cannot yet and leaves both columns null.

**Auth model**: both services are stateless OAuth2 resource servers validating Zitadel JWTs (`SecurityConfig`). Zitadel places project roles in the claim `urn:zitadel:iam:org:project:roles`; `ZitadelRoleConverter` turns those into Spring authorities, so endpoints guard with `@PreAuthorize("hasAuthority('platform_admin') or hasAuthority('game_master')")`. The three roles are `player`, `game_master`, `platform_admin` — kept in sync with the frontend's `PlatformRole` enum.

Both services share one Postgres instance, and each has its own DB user and its own schema (`svc_player`/`player`, `svc_commerce`/`commerce`), provisioned by `infrastructure/init-db.sh`. Each user owns its schema and has no rights in the other's. Flyway owns the schema and Hibernate only validates it (`ddl-auto: validate`, hardcoded); the schema each service targets is set by `spring.jpa.properties.hibernate.default_schema` and `spring.flyway.schemas`. service-commerce seeds `data.sql` when `SQL_INIT_MODE=always`.

Jacoco excludes `config/`, `dto/`, `mapper/`, `entity/Q*` (the generated QueryDSL metamodel), and `*Application`. Everything else we write is measured, including entities, exception handlers, and `security/`. The list is configured per-module in `pom.xml` and mirrored in `codecov.yml` — change both together, or the Codecov percentage stops matching the one the build reports. `platform-commons` excludes only `entity/Q*`; it holds no wiring, DTOs or mappers.

The package a class lives in decides whether it is measured, so logic does not go in a wiring package. That is why `ZitadelRoleConverter` sits in `security/` and not `config/`, and `PurchaseStatus` in `domain/` and not `entity/`. Both have unit tests that would otherwise score zero.

### Frontend

Angular 20 with standalone components (no NgModules), PrimeNG UI, `angular-oauth2-oidc`.

```text
core/auth/   → AuthService, authGuard (requires login), roleGuard (requires roles from route data),
               auth.interceptor (attaches bearer token), PlatformRole enum
core/api/    → typed HTTP clients per backend resource (player.service.ts, purchase.service.ts),
               models, playerProfileResolver
core/config/ → RUNTIME_CONFIG token and the config.json loader that fills it
features/    → one folder per routed feature (login, callback, dashboard, profile, players, purchases)
layout/      → MainLayoutComponent — authenticated shell
```

`app.routes.ts` nests all authenticated routes under `MainLayoutComponent` behind `authGuard`, with `playerProfileResolver` resolving once for the whole subtree; feature components are lazy-loaded via `loadComponent`. Role-gated routes (`/purchases`, `/players`) add `roleGuard` + `data: { roles: [...] }` — use this pattern for new role-restricted routes rather than checking roles inside components.

`frontend-portal/public/config.json` holds the Zitadel issuer, client id and API base URL. `zitadel-setup.sh` writes it, git ignores it, and `main.ts` fetches it before the app bootstraps, so nothing reads these values at import time. `src/environments/environment*.ts` keep only the `production` flag. The values must stay consistent with `infrastructure/.env` and the OAuth client registered in Zitadel. See ADR 0020.

Prettier config is inline in `package.json`: `singleQuote: true`, `printWidth: 100`, Angular parser for `.html`.

### Auth flow end-to-end

Traefik proxies Zitadel (API + v2 login UI) on port 8080. The frontend runs the OIDC code flow against Zitadel, then calls the backend services directly with the resulting JWT; backends only contact Zitadel via `issuer-uri` for token validation. Adding a protected backend endpoint means a `@PreAuthorize` check with the lowercase Zitadel role names; adding the corresponding frontend route means `roleGuard` with the matching `PlatformRole` values.

## Conventions

- Strict typing and explicit formatting are mandatory across the stack (per README).
- MegaLinter also runs shellcheck on `scripts/*.sh`, yamllint, markdownlint, hadolint, gitleaks, and checkov in CI — new shell scripts and YAML must pass those.
- use plain language, short sentences, and avoid dense or overly compressed phrasing.
- Commit messages are at most 10 lines: a subject, a blank line, and up to eight lines of body. Trailers do not count. If the reasoning does not fit, it was never commit-message material — it belongs in an ADR, and those are capped at 30 lines (`docs/adr/README.md`).
