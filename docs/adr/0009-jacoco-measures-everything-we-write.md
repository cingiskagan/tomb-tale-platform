# 9. Jacoco measures everything we write

- **Date:** 2026-09-07
- **Status:** accepted

## Context

Coverage is gated at 80%, but the exclusion list had drifted away from what it
claimed to do, and two of the things it hid were logic that already had tests.

`service-player` excluded `model/**`, a package that does not exist. So
`entity/**` was silently in the report, the generated QueryDSL metamodel counted
against us, and player coverage sat at 71.5% for no real reason. Both services
excluded `exception/**`, which holds branching logic. And `codecov.yml` listed
four `service-player` paths and nothing at all for `service-commerce`, so the
percentage Codecov published was not the one the build measured.

A gate you can move by editing a list is not a gate.

## Decision

We will exclude only code we did not write, and state the reason for each
exclusion:

- `config/**` — a bean definition either builds the context or the context
  fails. The slice tests prove that; a percentage adds nothing.
- `dto/**` — records with validation annotations and no method bodies.
  Validation is tested at the controller boundary, where the evidence is a 400,
  not a covered line.
- `mapper/**` — MapStruct writes the `*MapperImpl`. Covering it measures
  MapStruct.
- `entity/Q*` — QueryDSL metamodel, regenerated on every build.
- `*Application` — one `main()` that the smoke tests already run.

Everything else stays in the number, including entities, exception handlers and
`security/`. `codecov.yml` mirrors all five patterns for both services.

This has a corollary that constrains where code lives. **The package is the
coverage unit, so logic classes must live in logic packages.**
`ZitadelRoleConverter` moves from `config/` to `security/`, and `PurchaseStatus`
from `entity/` to `domain/`. Both are logic with their own unit tests, and both
were sitting in packages excluded for being wiring, scoring zero against a gate
they should have been raising.

## Consequences

- With no new tests written, the honest numbers were player 93.0% and commerce
  90.3%, both clear of the 80% gate. The old list was hiding good work as well as
  bad.
- The number now exposes real gaps instead of covering them:
  `GlobalExceptionHandler` sits at 35% because only
  `handleInvalidStatusTransition` is ever reached; `service-player`'s
  `ZitadelRoleConverter` is at 68% against commerce's 94%, because only commerce
  got a unit test for it; and `PlayerNotFoundException` is dead code.
- Two lists must change together from now on: `<excludes>` in each `pom.xml` and
  `ignore` in `codecov.yml`. Change one and the published percentage stops
  matching the one the build reports, which is the failure this record was
  written to end.
- New wiring goes in `config/`. New logic does not, however small it looks,
  because putting it there removes it from the gate without anyone deciding to.

---

*Implemented by:* `9e47260` chore: make the coverage gate honest
