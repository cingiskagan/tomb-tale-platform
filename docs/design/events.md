# Event schema

Read this before Commit E1. It fixes the shape of every published event.

## The envelope

```json
{
  "eventId": "8b1f0c4e-2a77-4f2e-9a2c-6d0f1b3c5e21",
  "eventType": "player.created",
  "eventVersion": 1,
  "occurredAt": "2026-09-23T10:15:30Z",
  "producer": "service-player",
  "data": {
    "playerPublicId": "3c9a6f10-0b44-4c8e-8f37-1d2a9c7b5e60",
    "displayName": "Player_a1b2c3d4",
    "characterPublicId": "77d2b381-5e19-4a6d-9c03-2f8ab41d7e55"
  }
}
```

Five envelope fields, plus a `data` object owned by the event type. The
producer creates `eventId` once, in the transaction that writes the outbox
row, and a retry reuses it. `service-audit` stores it as `_id`, so a redelivery
overwrites its own document ([data store research](data-store-research.md)).
`eventType` is the routing key on the `player.events` exchange. `occurredAt` is
when the fact happened, not when the publisher reached the broker.

## Rules

- Name a fact in the past tense. `player.created` is a fact, while
  `player.grant-starter-pack` is an order that moves an economy rule into the
  service that cannot see the economy.
- One event carries the player and the first character, because they commit
  together
  ([ADR 0012](../adr/0012-a-player-is-created-with-a-character-in-one-transaction.md)).
  A separate `character.created` waits for the second character.
- Payloads carry `publicId` values, never the Zitadel subject
  ([ADR 0014](../adr/0014-player-publicid-is-the-cross-service-identifier.md)).
  A published field cannot be withdrawn, so add one when a consumer needs it.
- Publish the facts another service acts on. Combat hits and page views are
  telemetry for the log pipeline.
- Inside a version, only add optional fields. A field that disappears or
  changes meaning needs `player.created.v2`, published beside v1 until the last
  consumer moves.
- The outbox delivers at least once. Deduplicate on a natural key with a unique
  constraint, not on a table of seen event ids.

## Who owns the beginner set

The event says a player exists, and the reward is consumer policy.
`service-inventory` owns the two healing potions and `service-commerce` owns
the 10 gold ([pathway](pathway.md)), so each grants its half on
`player.created`, keyed by the player and the grant code `beginner-set`. One
service holding the whole definition would have to tell the other to credit a
wallet, and that command needs a reply, a retry policy and a failure path, for
a grant worth 10 gold. Nothing consumes the event yet, so E1 ships the
producer alone.
