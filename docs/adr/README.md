# Architecture Decision Records

This directory holds the record of *why* the platform is built the way it is.

An Architecture Decision Record (ADR) is a short document about one decision. It
says what forced a choice, what we chose, and what that choice now costs. The
format is Michael Nygard's, and it is deliberately small. An ADR is a page, not
a design document.

`docs/design/` describes where the project is going. These records describe how
it got to where it is.

## The format

Every record has the same five parts. Start from `0000-template.md`.

- **Title** — a short phrase naming the decision, written as a statement.
- **Status** — `proposed`, `accepted`, `superseded by NNNN`, or `deprecated`.
- **Context** — what forced a choice: the pressure, the constraint, the thing
  that was broken. Write it without saying what you decided.
- **Decision** — what we chose, stated actively. "We will ..."
- **Consequences** — what is true now because of the choice. Both what it buys
  and what it costs. A record that lists no costs is usually not finished.

## The rules

**Records are append-only.** Once a record is accepted, its Context, Decision
and Consequences are never rewritten. Reality changing is not a reason to edit a
record. It is a reason to write the next one.

**A changed decision gets a new record.** Set the old one's status to
`superseded by NNNN` and link forward to it. The new record links back. That
chain is the history of the project, and it is why a record stays in the
directory even after it turns out to be wrong. Records 0004 and 0008 are the
worked example.

**Numbers are sequential and never reused.** They are identifiers, not
priorities.

**Write one when the choice would be expensive to reverse**, or when the next
person would reasonably ask "why is it like this?". Data ownership, boundaries
between services, the identity model, and the shape of the build all qualify. A
choice you would happily redo in an afternoon does not.

**A record is at most 30 lines.** A record nobody reads is worse than one that
left something out. Cut the Context to the pressure itself, state the Decision
as a rule, and keep Consequences to short bullets naming real costs. Records
0001 to 0019 predate this and are append-only like everything else here.

## A note on these records

Records 0001 to 0011 were written on 2026-09-08, after the fact. The dates are
the dates of the decisions, not of the writing. Records 0004 to 0009 are
reconstructed from commit messages that argued the case at the time, and each
one names the commit it came from. Records 0010 and 0011 are reconstructed from
the working notes that recorded those decisions when they were made. Records 0001
to 0003 are reconstructed from the code alone, so their Context is the honest
reasoning behind the shape of the code rather than a transcript of a discussion.

## Index

Sorted by the date of the decision. Numbers are assignment order, so they are not
in sequence here — a record written later can describe an earlier choice.

| # | Decision | Date | Status |
| --- | --- | --- | --- |
| [0001](0001-one-repository-for-the-whole-platform.md) | One repository for the whole platform | 2026-03-12 | accepted |
| [0002](0002-zitadel-is-the-only-identity-provider.md) | Zitadel is the only identity provider | 2026-03-12 | accepted |
| [0003](0003-public-uuid-separate-from-the-database-key.md) | A public UUID separate from the database key | 2026-06-30 | accepted |
| [0010](0010-integration-tests-are-the-default.md) | Integration tests are the default | 2026-07-10 | accepted |
| [0011](0011-purchases-are-an-admin-tool-for-now.md) | Purchases are an admin tool, so `playerId` stays in the body | 2026-08-12 | accepted |
| [0004](0004-pin-pmd-to-the-version-megalinter-ships.md) | Pin PMD to the version MegaLinter ships | 2026-08-24 | superseded by [0008](0008-the-maven-build-is-the-only-java-linter.md) |
| [0005](0005-flyway-owns-the-schema.md) | Flyway owns the schema | 2026-08-26 | accepted |
| [0006](0006-one-postgres-schema-and-user-per-service.md) | One Postgres schema and user per service | 2026-08-29 | accepted |
| [0007](0007-testcontainers-instead-of-mocked-repositories.md) | Testcontainers instead of mocked repositories | 2026-08-31 | accepted |
| [0008](0008-the-maven-build-is-the-only-java-linter.md) | The Maven build is the only Java linter | 2026-09-07 | accepted |
| [0009](0009-jacoco-measures-everything-we-write.md) | Jacoco measures everything we write | 2026-09-07 | accepted |
| [0012](0012-a-player-is-created-with-a-character-in-one-transaction.md) | A player is created with a character, in one transaction | 2026-09-09 | accepted |
| [0013](0013-every-entity-carries-the-same-six-fields.md) | Every entity carries the same six fields | 2026-09-11 | accepted |
| [0014](0014-player-publicid-is-the-cross-service-identifier.md) | `Player.publicId` is the cross-service player identifier | 2026-09-11 | accepted |
| [0015](0015-one-error-envelope-rfc-9457.md) | One error envelope, and it is RFC 9457 | 2026-09-14 | accepted |
| [0016](0016-lists-answer-with-a-paged-envelope.md) | Lists answer with a paged envelope of our own | 2026-09-17 | accepted |
| [0017](0017-an-api-field-is-named-after-the-field-behind-it.md) | An API field is named after the field behind it | 2026-09-17 | accepted |
| [0018](0018-zitadel-provisions-players-and-me-is-the-fallback.md) | Zitadel provisions players, and `/me` is the fallback that complains | 2026-09-17 | accepted |
| [0019](0019-zitadel-configuration-is-code.md) | Zitadel configuration is code | 2026-09-18 | accepted |
| [0020](0020-the-portal-reads-its-configuration-at-runtime.md) | The portal reads its configuration at runtime | 2026-09-19 | accepted |
