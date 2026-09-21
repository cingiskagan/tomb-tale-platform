# 4. Pin PMD to the version MegaLinter ships

- **Date:** 2026-08-24
- **Status:** superseded by [0008](0008-the-maven-build-is-the-only-java-linter.md)

## Context

Java static analysis ran twice against one ruleset: `./mvnw pmd:check` with
`${pmd.version}` locally, and MegaLinter's own PMD in CI. The versions differed,
and PMD rules change between releases, so CI failed on a branch that passed.

## Decision

We will pin `${pmd.version}` in both `pom.xml` files to the version the
MegaLinter image ships, currently 7.22.0, so both runs use one engine.

## Consequences

- Local runs and CI agree while the pin matches the image. Our PMD version is
  MegaLinter's choice now, so an image bump breaks the build with no change of
  ours. Dependabot cannot help: bumping `${pmd.version}` alone causes the
  disagreement it used to fix.
- Two tools still check the same Java, and neither Maven plugin is bound to a
  lifecycle phase, so the build enforces nothing. The failure returned on
  2026-09-04 with 7.26.0 against 7.27.0. See
  [0008](0008-the-maven-build-is-the-only-java-linter.md).

---

*Implemented by:* `1d17844` build(lint): run PMD 7.22.0 locally to match MegaLinter
