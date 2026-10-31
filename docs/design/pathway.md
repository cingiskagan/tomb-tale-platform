# Development Pathway: Tomb Tale Online RPG

This document orders the work and records the design each phase starts from. The [README](../../README.md#project-status) lists what runs today.

## Order

1. **Inventory:** `service-inventory` gets its catalog, item sets and coins, the portal writes them, and the game client shows the player's items.
2. **Town:** the first game server. Players meet in a town and see each other move, with their character names over their heads.
3. **Dungeon:** a party of up to 5 runs the combat loop in a dungeon copy, first on one fixed layout and then on layouts from `service-dungeon`.
4. **Later tiers:** the systems after Tier 1.

Tier 1 is the town and the dungeon. The [Tier 1 GDD](tier-1-game-design-document.md) says what the player does, the [Tier 1 TDD](tier-1-technical-design-document.md) says which piece does what, and [game server](game-server.md) records the network technology.

## Services

| Service | Database | Responsibility |
| --- | --- | --- |
| `service-player` | PostgreSQL | Player accounts, and their characters with name, level and experience. |
| `service-inventory` | PostgreSQL | The item catalog and item sets, the items players hold, coins for each character, and premium currency for each player. `jsonb` holds the stat pools that vary per item type. |
| `service-commerce` | PostgreSQL | Purchases, and later real-money payments and ad rewards. A purchase gets its items through the saga in [ADR 0027](../adr/0027-a-purchase-is-a-choreographed-saga-that-waits-in-pending.md). |
| `service-audit` | MongoDB | Domain events consumed from RabbitMQ. The only writer to MongoDB, and later the read API behind the player timeline. |
| `service-dungeon` | PostgreSQL | Procedural dungeon layouts built from room and corridor templates. `jsonb` holds the generated floors. |
| Game server | None | A Unity dedicated server with FishNet. One process runs every town and dungeon copy and keeps no permanent data. |

MongoDB holds domain events, never game state or logs, and `service-audit` is the only service that connects to it. Read [data store research](data-store-research.md) before inventory or dungeon work starts.

## 1. Inventory

- Create, edit and retire item templates in the portal, one at a time. A template defines base stats, random stat pools, rarity, its `assetKey` and an optional item set.
- Item sets, with a bonus for the complete set.
- A coin balance for each character. Premium currency belongs to the player, and waits until the game uses it.
- Give a player items with rolled stats from the portal.
- An admin view of a player's equipment and backpack.
- The game client's inventory screen, where the player sees the backpack and changes equipment.

Open question: coins belong to a character, but items belong to the player today (`item_instances.player_id`).

### Item Architecture: Template vs. Instance Pattern

Items use a two-layer design for easy rebalancing:

- **Item Template** (`item_templates` table): Admin-controlled blueprint defining base stats and random stat roll rules. Changing a template affects all players on next read.
- **Item Instance** (`item_instances` table): One owned stack. The row references its template, carries a `quantity` that exceeds one for stackable items, and stores the RNG-rolled modifiers. An item that rolls modifiers cannot stack, so its row has quantity 1.
- **Final Stats**: Computed at read time from the current template (`template base + rolled stats`), never stored. The version an instance records is provenance for its roll, not a pin, which is what allows global rebalancing without data migrations.

### Item Templates and Sets

Flyway owns the shape of `item_templates`, not its rows. Templates are content, and the portal creates, edits and retires them one at a time.

- A template is matched by a stable `code` such as `SWORD_IRON`, never by `publicId`. Purchases, other services and client assets name the code, so it never changes after creation.
- An edit sends the template version it started from. If the template changed since, the edit returns 409.
- A template is retired, never deleted. It stops dropping and leaves the shop, but copies that players own still resolve.
- Every create, edit and retire writes an event with the change to an outbox table, in the same transaction. The outbox publisher delivers it to `service-audit` through RabbitMQ, at least once ([events](events.md)). Without this, nothing keeps the balance history.
- A template can belong to one item set, such as `IRON_SET`. The set is a row of its own, with a `code` and a bonus. A player has the bonus while every template of the set is equipped. Like final stats, the bonus is computed at read time and never stored.
- Copying the whole catalog to a new environment is out of scope for now. That copy matches templates by `code`, because UUIDs differ between environments.

Definitions come first. You create a template before the client release that carries its asset, and creating it never checks what clients hold. The server refuses every operation on a `code` that has no template.

Each template carries an `assetKey`, the Addressables address of its asset, because artists move files and the address stays. A client with no asset for a template shows the item as a gray, generic object that the player cannot use. When a gray item is not acceptable, the platform forces a client update.

Clients know only the template rows, and so does the validator in Unity CI. It fails the build for an asset that no template uses, because the template must exist first. A template with no asset is only a warning.

Open questions: which credentials Unity CI uses to read the template rows, and whether a retired template still counts toward its set.

## 2. Town

The first real multiplayer test, built on the town in the [GDD](tier-1-game-design-document.md#1-town).

- The game server: a Unity dedicated server build with FishNet, in one process, with town copies of 20.
- The game server's own Keycloak client and `game_server` role, so it can read character names from `service-player`.
- Done when a phone and a desktop build see each other move in one town copy, each with its character's name. The [TDD](tier-1-technical-design-document.md#2-town-step) lists the checks.

## 3. Dungeon

- The combat loop of the [GDD](tier-1-game-design-document.md#2-dungeon), on one fixed layout written in the format `service-dungeon` will produce.
- Parties of up to 5 in dungeon copies of their own, and the way back to the town.
- Drops granted to inventory as they fall ([game server](game-server.md#item-grants)).
- `service-dungeon`: seeded generation from room and corridor templates, multithreaded floors (`CompletableFuture`, `ForkJoinPool`), batches of pre-generated seeds, and an Angular viewer for generated layouts. Its layouts then replace the fixed one, and the game server does not change.

## Gameplay Systems by Tier

### Tier 1: The Town and the Dungeon

- **Sessions:** login, one place per player, town copies, parties and dungeon copies.
- **Combat, AI, spawning and resources**, as the [GDD](tier-1-game-design-document.md) describes them.
- **Drops:** each drop goes into inventory when it falls.
- **UI:** login, the HUD and combat feedback.

### Tier 2: A Rewarding Loop

- **Skill system:** abilities, upgrades and specialization paths.
- **Timer system:** cooldowns, resets and live operations cycles.
- **Drop tables:** drop rates, rarity and distribution.

### Tier 3: The Meta-Game

- **Inventory limits:** storage and capacity.
- **Progression:** levels and unlocks.
- **Quests:** objectives, progression tasks and rewards.
- **Economy:** coin flow, prices and sinks.
- **Trading:** a bazaar and trades between players, during a run too. A listed item is locked, and an equipped item can't be listed or traded.
- **Social:** friends, chat and groups.

### Tier 4: Re-Evaluate

- **Crafting:** combining materials into items. It distracts from the combat loop, and monster drops are easier to balance.
- **Exploration:** discovering secrets. It matters less if dungeons are fast combat runs.
