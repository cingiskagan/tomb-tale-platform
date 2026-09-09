# 12. A player is created with a character, in one transaction

- **Date:** 2026-09-09
- **Status:** accepted

## Context

`GET /api/v1/players/me` was a write. `PlayerService.getOrCreatePlayer` ran
`backfillCharacterIfMissing` on every call: if the player it found had an empty
character list, it built a default character and saved it before returning.

That method arrived in `b6a8dd9`, the same commit that introduced
`GameCharacter`. The rows it repaired were real at the time — a local
development database, built by `ddl-auto: update`, holding players from before
characters existed. Then [0005](0005-flyway-owns-the-schema.md) gave Flyway the
schema and [0006](0006-one-postgres-schema-and-user-per-service.md) moved it
into a fresh `player` schema. Every database since has started empty. Nothing
has been deployed. The backfill had not repaired a row in two months and never
will again.

What it kept doing was standing in for a guarantee nobody had checked. "A player
always has a character" was true because a read path quietly made it true on the
next login, not because anything created players that way. A future code path
that forgot the character would have been repaired instead of reported, and the
repair would have looked like normal operation in the logs.

The list also planned a Flyway data migration to replace it. Against empty
tables that migration would insert zero rows and then live in the repository
forever as a step that never did anything.

## Decision

We will treat *a player always owns at least one character* as an invariant of
the creation path, held by a single transaction, and never repaired on read.

`createNewPlayerWithCharacter` builds the player and its first character and
passes both to one `playerRepository.save`. `Player.characters` is
`cascade = CascadeType.ALL`, so the two inserts belong to that one transaction:
both rows land or neither does. `getOrCreatePlayer` reads and, when it finds
nothing, creates. It does not write on the found branch.

`PlayerCreationAtomicityTest` holds the invariant to the database rather than to
an argument. It disables the transaction `@DataJpaTest` normally wraps a test in
— you cannot watch a rollback from inside the transaction being rolled back, and
production runs this path with no ambient transaction either — forces a unique
violation on the character insert, and asserts the players row is gone. It also
asserts the entity's IDENTITY key is non-null afterwards, which is the proof
that the row was really written first and then withdrawn, rather than never
attempted.

No data migration is written, because there is no data.

## Consequences

- Reading a profile is a read. The `/me` endpoint no longer takes a write path
  on a branch that was reached on every single request.
- **A characterless player is now possible and nothing will fix it.** Direct
  SQL, a second service writing to the table, a new creation path that forgets
  the character, or a future delete-character endpoint meeting `orphanRemoval`
  — any of those now produces a player the application will serve with an empty
  character list. That is the intended trade: the failure becomes visible
  instead of being silently repaired on the next login. It is still a real loss
  of tolerance, and it is the reason this record exists.
- Any new path that creates a `Player` must create its first character in the
  same transaction. Adding a way to delete a character means deciding, in that
  commit, what happens to the last one.
- `PlayerCreationAtomicityTest` commits real rows, so it cleans up after every
  test. It is the second class in the service that can pollute the shared
  Testcontainers instance — `ServicePlayerApplicationTests` is the first, for
  the same reason — and if its cleanup ever fails, the exact row counts in
  `PlayerQueryRepositoryImplTest` and `PlayerListQueryCountTest` fail instead,
  in whichever order Surefire happens to run them.
- `V2__characters_public_id_default.sql` gives `characters.public_id` the
  `DEFAULT gen_random_uuid()` that `players.public_id` already had, so the two
  halves of [0003](0003-public-uuid-separate-from-the-database-key.md) behave
  the same for a writer that is not Hibernate. It changes nothing for JPA:
  `GameCharacter.prePersist` still sets the UUID, and `ddl-auto: validate` does
  not inspect defaults.

---

*Implemented by:* `refactor(player): remove the login-time character backfill`
