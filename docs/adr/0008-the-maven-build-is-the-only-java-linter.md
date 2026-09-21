# 8. The Maven build is the only Java linter

- **Date:** 2026-09-07
- **Status:** accepted
- **Supersedes:** [0004](0004-pin-pmd-to-the-version-megalinter-ships.md)

## Context

MegaLinter had failed on `main` since 2026-09-04 while the same ruleset ran
clean locally: the pom pinned PMD 7.27.0 and the image shipped 7.26.0, the same
gap [0004](0004-pin-pmd-to-the-version-megalinter-ships.md) tried to pin away.
Neither Maven plugin was bound to a phase, so `verify` checked no Java style.

## Decision

We will let one engine check Java, and it is Maven. `checkstyle:check` and
`pmd:check` bind to `verify` in both poms, so `./mvnw clean verify` is the one
Java gate, at the pinned `${pmd.version}`. `JAVA_CHECKSTYLE` and `JAVA_PMD`
leave `.mega-linter.yml`.

## Consequences

- Local and CI cannot disagree: one engine at one pinned version for both.
- `verify` runs after code generation, so both tools read hand-written source only, like
  the Jacoco exclusions in [0009](0009-jacoco-measures-everything-we-write.md).
- MegaLinter no longer checks Java, and a third Java tool recreates this problem.

---

*Implemented by:* `9762a40` ci: make the Maven build the only Java linter
