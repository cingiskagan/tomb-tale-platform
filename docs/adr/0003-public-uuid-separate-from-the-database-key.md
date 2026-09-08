# 3. A public UUID separate from the database key

- **Date:** 2026-06-30
- **Status:** accepted

## Context

Entities use a generated numeric primary key. Exposing that key in the API makes
identifiers guessable. A URL ending in `/players/41` tells any caller that 40 and
42 probably exist, roughly how many players there are, and how fast that number
is growing. Walking the range is trivial, and every access check then has to be
perfect because discovery is free.

Making the primary key itself a UUID solves the leak but pays for it everywhere:
a wider key in every index, every foreign key and every join, with no ordering
locality on insert.

## Decision

We will give each entity two identifiers. The internal numeric primary key stays
as it is and never leaves the persistence layer. Alongside it, each entity
carries a `publicId` of type UUID, unique and not updatable.

Controllers, DTOs and URLs deal only in `publicId`.

## Consequences

- API identifiers reveal nothing about how many rows exist or in what order they
  were created.
- Joins and foreign keys keep a compact numeric key, so the cost of the UUID is
  one column and one index per table rather than the whole schema.
- Every lookup arriving from the API is a query on `publicId`. That column needs
  its unique index or each read becomes a scan.
- There are now two ways to say "which player", and they have different types.
  Repository and service signatures have to be explicit about which one they
  take, because `Long id` and `UUID publicId` are easy to swap by accident.
- The value is assigned in two places on purpose: `@ColumnDefault(
  "gen_random_uuid()")` covers rows inserted by SQL, and `@PrePersist` covers
  rows inserted through Hibernate. Seed data in `data.sql` never goes through the
  entity, so neither mechanism alone is enough.
