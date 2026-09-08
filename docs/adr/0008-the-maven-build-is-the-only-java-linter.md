# 8. The Maven build is the only Java linter

- **Date:** 2026-09-07
- **Status:** accepted

Supersedes [0004](0004-pin-pmd-to-the-version-megalinter-ships.md).

## Context

MegaLinter had been failing on `main` since 2026-09-04, reporting
`UnnecessaryWarningSuppression` against six test classes and calling their
`@SuppressWarnings("PMD.TooManyStaticImports")` pointless. Locally the same
ruleset was clean.

The two were not running the same PMD. The pom pinned 7.27.0, the MegaLinter
image shipped 7.26.0, and the rule changed behaviour between them.
`./mvnw pmd:check -Dpmd.version=7.26.0` reproduced the CI failure exactly. This is
the failure [0004](0004-pin-pmd-to-the-version-megalinter-ships.md) tried to
solve by pinning, returned with new version numbers, because pinning treats the
symptom.

The version gap turned out to be only half of it. Neither
`maven-checkstyle-plugin` nor `maven-pmd-plugin` had an `<executions>` block, so
neither was bound to a lifecycle phase. `./mvnw clean verify` ran Jacoco and
nothing else. The Test and Coverage job enforced no Java style or static analysis
at all. MegaLinter was the only thing checking Java in CI, at a version nobody
had chosen.

## Decision

We will let exactly one engine check Java, and it will be Maven.

`checkstyle:check` and `pmd:check` are bound to the `verify` phase in both poms,
so `./mvnw clean verify` is the single Java gate, running the version pinned in
`${pmd.version}`.

`JAVA_CHECKSTYLE` and `JAVA_PMD` are dropped from `.mega-linter.yml`. MegaLinter
keeps shell, YAML, Markdown, Dockerfiles, JSON, TypeScript, secrets and IaC — the
things Maven does not cover.

`scripts/pre-pr-tests.sh` runs `./mvnw clean verify` per service, the same
command CI runs.

## Consequences

- Local and CI cannot disagree about Java, because they are the same engine at
  the same pinned version. Dependabot moves that one number and both move
  together.
- Style failures now surface after the tests rather than before, because `verify`
  runs late. `./mvnw checkstyle:check pmd:check -DskipTests` stays the fast path
  while working, and `CLAUDE.md` says so.
- Binding to `verify` means generated sources exist by the time the checks run.
  Both tools immediately flagged the QueryDSL Q-classes and the MapStruct
  `*MapperImpl`: 4 Checkstyle errors and 14 PMD violations, none of them ours to
  fix. Both are now pointed at hand-written source only, for the same reason the
  Jacoco exclusions in [0009](0009-jacoco-measures-everything-we-write.md)
  exist.
- MegaLinter no longer looks at Java at all. Anything Checkstyle and PMD miss is
  now unchecked, and adding a third Java tool would recreate the problem this
  record exists to remove.
- The gate was verified to bite: an unused import in `LogUtils` fails
  `clean verify` on `UnusedImports`.

---

*Implemented by:* `9762a40` ci: make the Maven build the only Java linter
