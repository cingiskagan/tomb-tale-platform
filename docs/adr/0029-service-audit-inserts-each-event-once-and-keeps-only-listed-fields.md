# 29. service-audit inserts each event once, and keeps only listed fields

- **Date:** 2026-10-16
- **Status:** accepted

## Context

`events` keeps every domain event with no expiry, and service-audit is its only writer
([0026](0026-game-state-stays-in-postgres-and-mongodb-keeps-only-events.md)). The outbox delivers
at least once. Nothing deletes from `events`, so an erasure request cannot remove a personal field.

## Decision

We will give service-audit a MongoDB user that can only insert into `audit.events`, and people an
`audit_reader` that can only read. Each event's `_id` is its event id, so a redelivery fails and is
acknowledged. `EventAllowList` names the data fields per type and version, and the consumer drops
the rest and logs their names. An unlisted event goes to the DLQ with no retry. One queue takes
every routing key of `player.events`, `purchase.events` and `item.events`.

## Consequences

- A new event type, or a new field, reaches `events` only after a reviewed line in `EventAllowList`.
- An unlisted event waits in the DLQ. A copy without its data blocks a later replay, because the id is taken.
- service-audit cannot read `events`, so the timeline endpoint needs a reader role.
- The `*.unrouted` queues stay empty, because the audit queue takes every routing key.
- init-mongo.sh runs only on an empty volume, so an existing volume never gets the users.

---

*Implemented by:* `feat(audit): service-audit keeps the domain events in MongoDB`
