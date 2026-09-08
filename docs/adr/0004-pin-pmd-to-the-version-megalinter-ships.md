# 4. Pin PMD to the version MegaLinter ships

- **Date:** 2026-08-24
- **Status:** superseded by [0008](0008-the-maven-build-is-the-only-java-linter.md)

## Context

Java static analysis ran in two places against the same ruleset. Locally,
developers ran `./mvnw pmd:check`, which used the version in `${pmd.version}`.
In CI, MegaLinter ran PMD from the version baked into its Docker image.

The two versions were not the same, and PMD rules change behaviour between
releases. A branch could be clean locally and fail in CI on a rule that nobody
had touched, against a ruleset that had not changed.

## Decision

We will pin `${pmd.version}` in both `pom.xml` files to the version MegaLinter's
image ships, currently 7.22.0, so that the local run and CI use the same engine.

## Consequences

- Local runs and CI agree, for as long as the pin matches the image.
- Our PMD version is now MegaLinter's choice, not ours. Any MegaLinter image
  bump can break the build without a line of our code changing.
- Dependabot cannot help here. Bumping `${pmd.version}` on its own now *causes*
  the disagreement instead of fixing it, so the one number it could maintain is
  the one number that must stay frozen.
- The underlying problem is untouched: two separate tools are checking the same
  Java, and neither Maven plugin is bound to a lifecycle phase, so the Maven
  build does not actually enforce anything.

This held until 2026-09-04, when the same failure returned with 7.26.0 against
7.27.0. See [0008](0008-the-maven-build-is-the-only-java-linter.md).

---

*Implemented by:* `1d17844` build(lint): run PMD 7.22.0 locally to match MegaLinter
