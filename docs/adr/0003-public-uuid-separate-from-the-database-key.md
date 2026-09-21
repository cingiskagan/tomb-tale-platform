# 3. A public UUID separate from the database key

- **Date:** 2026-06-30
- **Status:** accepted

## Context

Entities use a generated numeric primary key. A URL ending in `/players/41`
tells any caller that 40 and 42 exist and how fast that number grows. A UUID
primary key stops the leak and pays for it in every index, key and join.

## Decision

We will give each entity two identifiers. The numeric primary key stays and
never leaves the persistence layer. Beside it, each entity carries a `publicId`
of type UUID, unique and not updatable. Controllers, DTOs and URLs deal only in
`publicId`.

## Consequences

- API identifiers say nothing about how many rows exist or in what order.
- Joins keep the compact numeric key, so the UUID costs one column and one
  unique index per table. That index is not optional: every lookup from the API
  queries `publicId`, and without it each read is a scan.
- `Long id` and `UUID publicId` are easy to swap by accident, so repository and
  service signatures must say which one they take.
- A row can arrive without Hibernate, so `@PrePersist` is not enough. A raw SQL
  insert needs a column default: `players.public_id` has one,
  `characters.public_id` does not, and `Player`'s `@ColumnDefault` does nothing
  now that Flyway owns the schema ([0005](0005-flyway-owns-the-schema.md)).
