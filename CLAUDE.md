# CLAUDE.md

## Overview

Coins and premium currency belong to service-inventory, not service-commerce.

The root `pom.xml` is an aggregator over `platform-commons`, the four services and the Keycloak mapper. It is not their parent — the services and `platform-commons` keep `spring-boot-starter-parent`, and the mapper has none — and exists only so Maven resolves `platform-commons` from the reactor. Build Java from the repository root.

## Common Commands

### Infrastructure (start first)

```bash
cd infrastructure
docker compose up -d
```

All service ports, credentials, and Keycloak settings come from `infrastructure/.env` (copy from `.env.example`). Keycloak's realm comes from `infrastructure/keycloak/import/tombtale-realm.json`.

### Backend services (service-player, service-commerce, service-inventory, service-audit)

```bash
cd service-player            # or service-commerce, service-inventory, service-audit
./run-local.sh               # full mode: real Postgres (MongoDB for audit) + RabbitMQ from Docker
```

From the repository root, one launcher starts them one after another:

```bash
./scripts/run-all-local.sh                    # all four
./scripts/run-all-local.sh player inventory   # only the ones named
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

Both `check` goals are bound to the `verify` phase, so `./mvnw clean verify` is the single gate: the same engine at the same pinned version runs locally and in CI. MegaLinter does not lint Java — it ships its own PMD build, and when that drifted from `${pmd.version}` the two disagreed about the same ruleset. Style failures surface after the tests as a result; `./mvnw -pl <module> -am verify -DskipTests` is still the fast path while you are working. Naming the two goals directly — `checkstyle:check pmd:check` — no longer works from a clean tree: a goal invoked without a lifecycle phase never builds `platform-commons`, so resolving the module's dependencies fails before either check runs. `verify -DskipTests` reaches the same two goals, since both are bound to that phase.

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

The services follow an identical layering: copy the sibling service's pattern when adding anything new.

Every entity extends `BaseEntity` from `platform-commons`, which carries the same six fields everywhere: `Long id` (internal, never leaves the persistence layer), `UUID publicId` (the only identifier in an API), `createdAt`/`updatedAt`, and `createdBy`/`updatedBy`. Controllers and DTOs deal only in `publicId`. See ADR 0013.

`publicId` is assigned where the field is declared, not in a `@PrePersist`, so it exists before the first save and equality can depend on it — `BaseEntity` defines `equals`/`hashCode` on it, and entities must not generate their own. Lombok follows from that: `@SuperBuilder` instead of `@Builder`, explicit `@Getter`/`@Setter` instead of `@Data`.

The actor columns come from Spring Data auditing. Every Postgres service wires `PlatformAuditorAware` from `platform-commons` in `config/JpaConfig`. It names the token's `public_id` claim, or the `SystemActor` that a consumer or job runs as through `SystemActor.run`, and refuses any other write with a 403. Start a job's transaction inside `run`, because Hibernate asks for the author at the commit.

**Purchase saga**: a purchase crosses two services with no shared transaction (ADR 0027). Commerce saves it as `PENDING` and writes `purchase.created` through its outbox. Inventory saves one `ItemGrant` per purchase and sends it as `item.granted` or `item.grant-rejected`. Commerce then moves the purchase to `COMPLETED` or `CANCELLED`, keeps the rejection's reason in `cancellation_reason`, and only those two events move a purchase out of `PENDING`.

**Tracing**: Micrometer Tracing over OpenTelemetry. Nothing is exported until Phase G picks a backend (ADR 0028). A log line written inside a request or a message carries the trace id. RabbitMQ messages carry the W3C `traceparent`. The outbox row stores it, so the later send from the publisher continues the same trace. Tests turn trace export off, and that also stops propagation. A test that checks a trace needs `@AutoConfigureTracing`.

**Auth model**: every service with an API is a stateless OAuth2 resource server validating Keycloak JWTs (`SecurityConfig`). Keycloak lists realm roles in the flat `roles` claim; `RoleClaimConverter` in `platform-commons` turns those into Spring authorities, so endpoints guard with `@PreAuthorize("hasAuthority('platform_admin') or hasAuthority('game_master')")`. The three roles are `player`, `game_master`, `platform_admin` — kept in sync with the frontend's `PlatformRole` enum. Access tokens are lightweight: a claim reaches the services only if its mapper in the realm file sets `lightweight.claim`, as `sub`, `roles` and `public_id` do.

The services share one Postgres instance, and each has its own DB user and its own schema (`service_player`/`player`, `service_commerce`/`commerce`, `service_inventory`/`inventory`), provisioned by `infrastructure/init-db.sh`. Each user owns its schema and has no rights in the others. Flyway owns the schema and Hibernate only validates it (`ddl-auto: validate`, hardcoded); the schema each service targets is set by `spring.jpa.properties.hibernate.default_schema` and `spring.flyway.schemas`.

service-audit has no Postgres. It writes MongoDB's `audit.events` as `service_audit`, a user that can only insert, created by `infrastructure/init-mongo.sh` (ADR 0029). `EventAllowList` names the data fields each event type keeps. An event type with no list waits in the DLQ. People read the events as `audit_reader`, which can only read, for example in MongoDB Compass.

Jacoco excludes `config/`, `dto/`, `mapper/`, `entity/Q*` (the generated QueryDSL metamodel), and `*Application`. Everything else we write is measured, including entities, exception handlers, and `security/`. The list is configured per-module in `pom.xml` and mirrored in `codecov.yml` — change both together, or the Codecov percentage stops matching the one the build reports. `platform-commons` excludes only `entity/Q*`; it holds no wiring, DTOs or mappers. `service-audit` excludes `config/`, `dto/` and `*Application`, because it has no QueryDSL or MapStruct.

The package a class lives in decides whether it is measured, so logic does not go in a wiring package. That is why `RoleClaimConverter` sits in `security/` and not `config/`, and `PurchaseStatus` in `domain/` and not `entity/`. Both have unit tests that would otherwise score zero.

### Auth flow end-to-end

nginx on port 8080 fronts both: `/api` goes to the backend services, `/realms/` and `/resources/` to Keycloak, and any other path gets a 404. `infrastructure/README.md` lists the routes, and a new controller prefix needs its `location` in `infrastructure/nginx.conf` (ADR 0025). The frontend runs the OIDC code flow against Keycloak, then calls the backend services through nginx with the resulting JWT. Backends only contact Keycloak via `issuer-uri` for token validation. Adding a protected backend endpoint means a `@PreAuthorize` check with the lowercase realm role names; adding the corresponding frontend route means `roleGuard` with the matching `PlatformRole` values.

## Conventions

- Commit messages are at most 10 lines: a subject, a blank line, and up to eight lines of body. Trailers do not count. If the reasoning does not fit, it was never commit-message material — it belongs in an ADR, and those are capped at 30 lines (`docs/adr/README.md`).
