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
- Assigning the value needs two mechanisms, because a row can arrive two ways.
  `@PrePersist` fills `publicId` for everything Hibernate inserts, which is
  everything the application itself creates. A row inserted by raw SQL — seed
  data, or a data migration — never touches the entity, so it needs a column
  default in the migration instead.
- Only one table has both today. `players.public_id` is declared
  `DEFAULT gen_random_uuid()` in `V1__baseline.sql`; `characters.public_id` is
  not, and would fail its `NOT NULL` constraint if anything ever SQL-inserted a
  character. Nothing does, so this is latent rather than broken.
- `Player` also carries `@ColumnDefault("gen_random_uuid()")`, which does nothing
  at runtime. It is a schema-generation annotation, and Flyway owns the schema
  while Hibernate only validates it — see
  [0005](0005-flyway-owns-the-schema.md). It is a leftover from the
  `ddl-auto: update` era the baseline was dumped from, and it is why the
  `players` default exists while the `characters` one does not: `GameCharacter`
  never had the annotation.
