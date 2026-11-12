# Inventory design

The inventory phase's decisions as of 2026-11-03. The other design documents give the general picture and point here, and where they differ, this document wins. The item model, with templates, instances and sets, is in the [pathway](pathway.md#1-inventory).

## Scope

- **The portal, in this repo:** a debug panel for `platform_admin` only, never used beside a live game server. It manages item templates, each with an image, and NPC dealers with the items on their stands. An admin opens any character's inventory beside the stand of a dealer they pick, with no separate general shop, and trades by drag and drop.
- **The Unity project:** the game's inventory screen. It needs only the platform, so it can start once inventory answers a read. Only showing new equipment to other players waits for the town.

## Decided

### Who calls inventory

- The game client calls `service-inventory` directly with its own token, for its own character: it reads the inventory, changes equipment and trades with dealers. Inventory checks the price, the balance and the owner itself.
- The game token reaches many platform endpoints, not only `/players/me`.
- The game server reads inventory and grants drops. Only the game server grants from play ([ADR 0030](../adr/0030-the-game-is-a-public-client-with-offline-sessions-and-touches-only-its-own-data.md)), and the client acts on its own character directly ([ADR 0031](../adr/0031-the-game-acts-on-its-own-characters-inventory-directly.md)).
- An item effect inside the world, like a potion in Tier 2, goes through the game server, because the effect happens there.
- One set of endpoints, with the character in the path, serves every caller: `platform_admin` for any character, `player` for its own, and `game_server` for reads and grants.
- Equipment may change during a run too. When anything in a player's inventory changes, inventory informs the game server, which updates the player's stats, so the server never relies on the client to report a change.
- Inventory informs it with a thin `inventory.changed` event, carrying only the character's id, through its outbox and RabbitMQ. The server then reads the equipment over REST, so a repeated or late event does no harm.
- Until the event arrives, combat uses the stats the server read last. The outbox publishes every 5 seconds, so that usually lasts a few seconds, and while RabbitMQ is down it lasts until RabbitMQ is back. A running server loses no event, because the outbox delivers at least once, and a server that restarts reads every connected player's equipment again.
- RabbitMQ's .NET client must work in the IL2CPP server build, and a short test before the town step checks it. If it doesn't, the server asks inventory for its players' equipment every few seconds instead.

### The dungeon lock

- A player in a dungeon holds a dungeon lock. It lives on the `character_inventories` row as a lease: the run's id and the time it lasts until. Other services read it through inventory's API.
- The game server sets it when a run starts, renews it for the party every 30 seconds and clears it when the run ends. If the server crashes, the renewals stop and the lock ends within about two minutes.
- A renewal or a clear names its run, and inventory ignores one for another run, so one game server can't end a lock another server holds. Inventory sets the end time from its own clock.
- Marketplace actions check the lock under the character lock, so a run can't start between the check and the action.
- Inside a dungeon, party members may trade items with each other, because the party is isolated from the outer world. Both characters must hold the lock for the same run, so a server can't trade between two dungeons.
- The lock blocks what crosses the dungeon's border: the dealer, the marketplace, and any later stash or transfer. Equips and bag moves stay open to the player, because items only move within the character.
- `platform_admin` changes nothing while the lock is on. In production, the portal warns an admin before they change a player's inventory.

### NPCs and the marketplace

- Clicking an NPC checks the player's dungeon lock, position and map, such as the town.
- Inventory refuses a dealer trade while the dungeon lock is on. Position and map are checked by the client only, because inventory can't see them, and shopping from across town at fixed prices gains a cheater nothing. If a position starts to matter, the game server gives the client a short ticket after a valid click, and inventory asks for it.
- Each dealer has a stable `code`, such as `TOWN_BLACKSMITH`, because the Unity town places the NPC and finds its stand by that code. A dealer is retired, never deleted.
- The marketplace (Tier 3) lives in `service-inventory`, beside the items and coins, so a sale is one transaction.
- A seller gives an NPC the item and a price. The item leaves the seller's bag, and the NPC lists it to the other players who click it.
- Every marketplace action checks the lock, so a player lists an item only from town.
- A player sees their own listings and can cancel one, which puts the item back in the bag, but can't buy one.
- A listing is active, sold or cancelled. A buy or a cancel changes it only while it is active, in one conditional update, so a listing sells once and the second caller hears "already sold".
- A sale changes two characters, so it takes both character locks. Every change that touches two characters, party trades too, takes them in the same order, the smaller character id first, so two such changes can't deadlock.

### Ownership

- A character owns its items and its coins. Only premium currency belongs to the player, and it waits until the game uses it.
- Inventory keeps a `character_inventories` table, one row per character with only what inventory needs: the character's `publicId`, its owner's `publicId`, its coins and a `version`. Names and levels stay in service-player.
- Inventory fills it from `player.created`, because its database user has no rights in service-player's schema ([ADR 0006](../adr/0006-one-postgres-schema-and-user-per-service.md)). The row lets inventory check that a player owns the character they call for.
- An item row points at its character's row with a foreign key. A purchase names only the player. Until the player can pick the character that claims it, inventory gives it to the player's only character and refuses it when the player has none or several.

### The character lock

- Every change to a character's inventory first locks its `character_inventories` row with `SELECT … FOR UPDATE`. A second change for the same character waits until the first commits, then reads its result.
- The lock doesn't touch `item_instances`. It guards them because every change takes it first. That matters for rules that span several rows, or a row that doesn't exist yet: a two-handed weapon and a shield, the lowest free cell, the coins.
- Taking the lock writes nothing, so the row's version moves only when its coins do.
- One method takes the lock, and every write path goes through it: the portal, the game client, the game server's drops and the purchase consumer.

### Items

- An item instance has a `rarity` and a `bonus`, the bonus stats it rolled, as JSON. The rarities, from lowest to highest, are gray, green, blue, purple, orange and red.
- Each rarity adds one bonus: gray rolls none, green 1, blue 2, purple 3, orange 4 and red 5.
- Copies stack only when they share the template and the rarity and have no bonus.
- Every template has an `item_type`, such as helmet, so equipping can check that an item fits its slot.
- Item sets with their set bonus are in this phase, as the pathway describes them.
- A retired template stays in its set, so an owned copy still counts and retiring a piece never takes a bonus away. To make a set need fewer pieces, an admin takes the template out of the set.

### Icons and names

- Each template has one icon and one `name`, and the portal and Unity both show them. When translations come, the platform adds a name per language.
- Inventory stores the icons in Postgres, in a table of their own, and serves them with a long cache time. An icon's URL carries the image's hash, so a client keeps its copy until the icon changes.
- An upload must be PNG or WebP, checked by the file's bytes rather than its name, and stay under a size limit. An SVG can carry script, and the API shares its origin with Keycloak.
- A client that can't load an icon shows a generic one.

### Damage types

- The damage types are physical, fire, ice, dread and divine, and players deal all five.
- A creature's nature sets what it is weak to and what it resists. Physical has no weakness, and armor reduces it.

| Nature | Weak to (×1.5) | Resists (×0.5) |
| --- | --- | --- |
| Fire | ice | fire |
| Ice | fire | ice |
| Dark: undead, wraiths | divine | dread |
| Holy | dread | divine |
| Living: beasts, bandits | none | none |

The chart lives here, and the GDD points to it. Its multipliers are data in the game server, so they change without code. Dread works like any other type and only looks dreadful. Dark creatures resist it, so the holy enemies it needs come from a faction such as heretics.

### Bonus stats

- A bonus rolls from 15 stats:
  - Offense: physical, fire, ice, dread and divine damage, critical chance and critical damage.
  - Defense: armor, max HP, and fire, ice, dread and divine resistance.
  - Stamina: max stamina and stamina regeneration.
- Each item type has one pool, with a range for each stat. Weapons roll damage and critical stats, armor pieces and shields roll armor, HP, resistances and stamina, and neck and rings roll any stat.
- A bonus stores stat names with values, such as `{"FIRE_DAMAGE": 12, "CRIT_CHANCE": 3}`, and inventory refuses a stat it doesn't know.
- Attack speed, cooldown reduction, gold find, HP regeneration and movement speed wait for later tiers.

### Equipment

- A nullable `equipment_slot` on `item_instances` holds an equipped item's slot. A unique constraint on character and slot refuses two items in one slot, and empty slots never collide, because a Postgres unique constraint ignores empty values.
- A character has nine slots. Neck and rings show no model on the character, only an icon.

| Slot | Takes |
| --- | --- |
| Main hand | one-handed or two-handed weapon |
| Off hand | shield |
| Head | helmet |
| Chest | armor |
| Hands | gloves |
| Feet | boots |
| Neck | amulet |
| Ring 1, ring 2 | ring |

Equipping runs in one transaction under the character lock, so two taps at once can't both pass the checks:

1. The item belongs to the character and is in the bag.
2. The slot takes the item's type.
3. An item already in the slot goes back to the bag.
4. A two-handed weapon also sends the off-hand item to the bag, and a shield sends a two-handed weapon to the bag.
5. After the commit, inventory informs the game server.

### Bag

- Every item in the bag has a `bag_position`, its cell in the grid, and an equipped item has none. A cell holds one item or one stack.
- The position is one number, not a row and a column, so the client chooses how many columns to draw.
- A new item or stack takes the lowest free cell. An item that leaves a slot takes the cell of the item that replaces it, or else the lowest free cell.
- The character lock keeps two changes at once from taking the same cell.
- Both unique constraints, on the slot and on the cell, are checked at the commit, so a swap of two items doesn't collide halfway.
- The bag has no limit yet. When Tier 2 adds one, every path that adds an item needs an answer for a full bag: drops, purchases, dealer buys, unequipping and party trades.
- `inventory.bag-cells` sets the bag's size. A move already stays inside it, so a client can't scatter items, and Tier 2 makes it that limit.

### Prices

- One price per template. A dealer's stand only lists the items that dealer sells.
- A stand can sell any rarity. A bought item rolls its rarity's bonuses from its item type's pool, like any other.
- To test bonuses, an admin puts a custom item on a stand, with a rarity and a bonus they choose. It never sells out, and every copy a player buys carries exactly that bonus, so testers can try any scenario.
- In production, admins don't add rare items. A setting, `inventory.custom-stand-items.enabled`, is off by default, and `.env.example` turns it on for the local stack. While it's off, the endpoint refuses the action and the portal hides the button.
- A scheduled job puts random rare items on stands in production, through its own code, so the setting doesn't affect it.
- Each rarity triples the price: green costs 3 times gray, blue 9, purple 27, orange 81 and red 243.
- Selling to any dealer pays a fixed share of the item's price, `inventory.sell-share-percent`, rounded down to whole coins. So no route between dealers makes coins, and an item that no dealer sells still has a sell price.

### Coins

- A character's coins are a count on its `character_inventories` row.
- Inventory creates that row when `player.created` arrives, once per character, so a redelivered event adds nothing.
- Every new character starts with 100000 coins. The amount is a setting, lowered before the game is published.
- Every coin change also writes a history row in the same transaction, with the amount, the reason and a unique reference such as the trade id. That covers buys, sells, drops and the starting coins.
- Prices and counts are `bigint`, because a red item costs 243 times its template's price.

### Trade safety

- A buy sent twice charges once. The client makes a trade id for each buy, and the history row stores it under a unique constraint, so a repeated request finds the first trade.
- The buy sends the price the player saw. If the price changed since, inventory answers 409.
- Two buys at the same moment can't overdraw, because the character lock makes the second one wait. The row's `@Version` and `CHECK (coins >= 0)` are the backups.
