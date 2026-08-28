# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Monorepo for the "Tomb Tale Online RPG" platform: two Spring Boot microservices, an Angular web portal, and Docker Compose infrastructure to run everything locally.

- `frontend-portal/` — Angular 20 portal (standalone components, PrimeNG, OIDC auth via Zitadel)
- `service-player/` — Spring Boot service for player accounts and characters (port 8081)
- `service-commerce/` — Spring Boot service for purchases/economy (port 8082)
- `infrastructure/` — Docker Compose stack: Traefik, Zitadel (auth), Postgres, Redis, MongoDB, RabbitMQ
- `config/checkstyle/`, `config/pmd/` — static-analysis rulesets shared by both Java services
- `scripts/pre-pr-tests.sh` — full local pre-PR check pipeline across all three modules

Both services use Java 21 and Spring Boot 4. The frontend requires the Node version pinned in `frontend-portal/.nvmrc`.

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
./run-local.sh --test        # test mode: H2 in-memory, no Docker deps
```

Maven, from within each service directory:

```bash
./mvnw clean test                                       # unit tests
./mvnw test -Dtest=CharacterServiceTest                 # single test class
./mvnw test -Dtest=CharacterServiceTest#methodName      # single test method
./mvnw checkstyle:check pmd:check -DskipTests           # style/static analysis only
./mvnw clean verify                                     # tests + Jacoco report (what CI runs)
```

Checkstyle/PMD rulesets live in `config/checkstyle/checkstyle.xml` and `config/pmd/pmd-ruleset.xml`, referenced relatively from each `pom.xml` — one copy governs both services.

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
./scripts/pre-pr-tests.sh                              # all modules
./scripts/pre-pr-tests.sh --dir-name service-player    # one module only
./scripts/pre-pr-tests.sh --clean                      # also runs npm ci
```

Run this before opening a PR — CI (`test-and-coverage.yml`, `mega-linter.yml`) enforces the same checks, plus Codecov coverage upload.

## Architecture

### Backend services

Both services follow an identical layering — copy the sibling service's pattern when adding anything new:

```text
controller/  → REST endpoints under /api/v1/..., @PreAuthorize role checks
service/     → business logic, transactional boundaries
repository/  → Spring Data JPA; dynamic filtering via QueryDSL (…QueryRepository + …QueryRepositoryImpl)
entity/      → JPA entities (service-player uses entity/, coverage config also references model/)
dto/         → request/response DTOs with jakarta validation
mapper/      → MapStruct compile-time mappers between entities and DTOs
exception/   → domain exceptions; service-commerce has a GlobalExceptionHandler
config/      → SecurityConfig, QueryDslConfig, RabbitMQConfig, ZitadelRoleConverter
```

Entities expose a public-facing `publicId` (UUID) distinct from the internal DB primary key — controllers and DTOs deal only in `publicId`.

**Auth model**: both services are stateless OAuth2 resource servers validating Zitadel JWTs (`SecurityConfig`). Zitadel places project roles in the claim `urn:zitadel:iam:org:project:roles`; `ZitadelRoleConverter` turns those into Spring authorities, so endpoints guard with `@PreAuthorize("hasAuthority('platform_admin') or hasAuthority('game_master')")`. The three roles are `player`, `game_master`, `platform_admin` — kept in sync with the frontend's `PlatformRole` enum.

Both services share one Postgres instance, and each has its own DB user and its own schema (`svc_player`/`player`, `svc_commerce`/`commerce`), provisioned by `infrastructure/init-db.sh`. Each user owns its schema and has no rights in the other's. Flyway owns the schema and Hibernate only validates it (`ddl-auto: validate`, hardcoded); the schema each service targets is set by `spring.jpa.properties.hibernate.default_schema` and `spring.flyway.schemas`. service-commerce seeds `data.sql` when `SQL_INIT_MODE=always`.

Jacoco excludes `config/`, `entity|model/`, `dto/`, `exception/`, `mapper/`, and `*Application` from coverage (configured per-service in `pom.xml`) — coverage targets land on controllers, services, and repository impls.

### Frontend

Angular 20 with standalone components (no NgModules), PrimeNG UI, `angular-oauth2-oidc`.

```text
core/auth/   → AuthService, authGuard (requires login), roleGuard (requires roles from route data),
               auth.interceptor (attaches bearer token), PlatformRole enum
core/api/    → typed HTTP clients per backend resource (player.service.ts, purchase.service.ts),
               models, playerProfileResolver
features/    → one folder per routed feature (login, callback, dashboard, profile, players, purchases)
layout/      → MainLayoutComponent — authenticated shell
```

`app.routes.ts` nests all authenticated routes under `MainLayoutComponent` behind `authGuard`, with `playerProfileResolver` resolving once for the whole subtree; feature components are lazy-loaded via `loadComponent`. Role-gated routes (`/purchases`, `/players`) add `roleGuard` + `data: { roles: [...] }` — use this pattern for new role-restricted routes rather than checking roles inside components.

`src/environments/environment*.ts` hold the Zitadel issuer/client id and API base URL; these must stay consistent with `infrastructure/.env` and the OAuth client registered in Zitadel.

Prettier config is inline in `package.json`: `singleQuote: true`, `printWidth: 100`, Angular parser for `.html`.

### Auth flow end-to-end

Traefik proxies Zitadel (API + v2 login UI) on port 8080. The frontend runs the OIDC code flow against Zitadel, then calls the backend services directly with the resulting JWT; backends only contact Zitadel via `issuer-uri` for token validation. Adding a protected backend endpoint means a `@PreAuthorize` check with the lowercase Zitadel role names; adding the corresponding frontend route means `roleGuard` with the matching `PlatformRole` values.

## Conventions

- Strict typing and explicit formatting are mandatory across the stack (per README).
- MegaLinter also runs shellcheck on `scripts/*.sh`, yamllint, markdownlint, hadolint, gitleaks, and checkov in CI — new shell scripts and YAML must pass those.
- use plain language, short sentences, and avoid dense or overly compressed phrasing.
