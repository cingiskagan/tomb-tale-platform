# 9. Jacoco measures everything we write

- **Date:** 2026-09-07
- **Status:** accepted

## Context

Coverage is gated at 80%, but the exclusion list had drifted. `service-player` excluded
`model/**`, a package that does not exist. Both services excluded `exception/**`, which
holds branching logic, and `codecov.yml` listed paths that matched neither pom.

## Decision

We will exclude only code we did not write: `config/**`, `dto/**`, `mapper/**`,
`entity/Q*` and `*Application`. Everything else is measured, including entities,
exception handlers and `security/`. The package is the coverage unit, so logic
lives in a logic package: `ZitadelRoleConverter` moves to `security/` and
`PurchaseStatus` to `domain/`.

## Consequences

- With no new tests the numbers were player 93.0% and commerce 90.3%, and real
  gaps show now: `GlobalExceptionHandler` at 35%, `PlayerNotFoundException` dead.
- Two lists change together: `<excludes>` in each `pom.xml` and `ignore` in
  `codecov.yml`. New wiring goes in `config/`, and new logic does not, however
  small it looks, because that hides it from the gate.

---

*Implemented by:* `9e47260` chore: make the coverage gate honest
