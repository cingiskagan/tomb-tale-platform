# 31. The game acts on its own character's inventory directly

- **Date:** 2026-11-02
- **Status:** accepted
- **Supersedes:** [0030](0030-the-game-is-a-public-client-with-offline-sessions-and-touches-only-its-own-data.md)

## Context

[0030](0030-the-game-is-a-public-client-with-offline-sessions-and-touches-only-its-own-data.md)
left a game token one platform endpoint, `/players/me`, and every grant to a game server.
The game client must now read its inventory, change equipment and trade with dealers.
Sending that through the game server adds a hop, and no check that inventory can't make.

## Decision

We will let a `player` token call service-inventory directly for its own character: reads,
equipment, bag moves, dealer trades and the marketplace. Inventory checks the price, the
balance, the owner and the dungeon lock itself. Grants from play stay with the game server,
and inventory tells it about every change with an `inventory.changed` event.

## Consequences

- 0030's line that a game token reaches only `POST` and `PATCH /players/me` no longer
  holds. The rest of 0030 stands.
- Inventory's write endpoints face the internet, so the nginx rate limit in the backlog
  matters sooner.
- Every write checks that the token's `public_id` owns the character, through the
  `character_inventories` row ([inventory design](../design/inventory-design.md)).
- Position and map are checked only by the client, until a dealer's place matters.
