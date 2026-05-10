# Development Pathway: Tomb Tale Online RPG

This document categorizes both the standard game design systems and the necessary technical backend systems based on their implementation priority. Development follows a **platform-first strategy**: build backend microservices and Angular admin tools before writing any Unity game client code.

---

## 🏗️ Platform-First: Microservice Architecture

| Microservice | Database | Responsibility |
|---|---|---|
| **service-player** | PostgreSQL | Player accounts, core stats (level, XP, base attributes). Fixed relational schema. |
| **service-inventory** | MongoDB | Item catalog templates, player-owned item instances (template + rolled stats pattern). Document DB for flexible item schemas. |
| **service-dungeon** | MongoDB | Procedural dungeon generation, room/corridor templates, seed management. Multithreaded floor generation. |
| **service-commerce** | PostgreSQL | Shop storefront, virtual currency wallets, purchase transactions. *(Already exists)* |

### Item Architecture: Template vs. Instance Pattern

Items use a two-layer design for easy rebalancing:

*   **Item Template** (`item_templates` collection): Admin-controlled blueprint defining base stats and random stat roll rules. Changing a template affects all players on next read.
*   **Item Instance** (`item_instances` collection): The player's unique copy storing only the RNG-rolled modifiers and a reference to the template.
*   **Final Stats**: Computed at read time (`template base + rolled stats`), never stored. This allows global rebalancing without data migrations.

---

## 📋 Platform Development Phases (No Unity Required)

### Phase 1: Player Management (`service-player`)
*   Player list with search and filtering (Angular + PrimeNG)
*   View and edit player stats (add/subtract XP, adjust level)
*   Full player profile admin panel

### Phase 2: Inventory & Item Catalog (`service-inventory`)
*   Item template CRUD (define base stats, random stat pools, rarity)
*   Assign item instances to players with rolled stats
*   Admin view: inspect a player's full equipment and backpack
*   Template versioning for rebalancing

### Phase 3: Procedural Dungeon Generation (`service-dungeon`)
*   Room and corridor template definitions
*   Seed-based procedural generation algorithm
*   Multithreaded floor generation (`CompletableFuture`, `ForkJoinPool`)
*   Angular test UI to visualize generated dungeon layouts in real-time
*   Batch pre-generation of dungeon seeds

---

## 🔌 Core Infrastructure: Online Capabilities (Prerequisites)
*Mandatory to build the foundation for an online multiplayer game.*

*   **Networking & Replication System**: Manages server-client communication, latency compensation, and state synchronization.
*   **Matchmaking & Session System**: Groups players together and connects them to dedicated dungeon instance servers.
*   **Persistence & Database System**: Securely saves character progression, inventory, and stats to backend microservices (so players can't cheat/hack their data).
*   **UI/UX System**: Provides the interface for main menus, matchmaking lobbies, in-game HUDs, and combat feedback.

## 🚨 Tier 1: Absolute Must-Haves (The Core Gameplay Loop MVP)
*The required gameplay systems to make two entities successfully fight in a server instance.*

*   **Combat system**: Controls how players fight, deal damage, and resolve encounters.
*   **AI behavior system**: Defines how NPCs act, react, and make gameplay decisions.
*   **Spawning system**: Controls placement of enemies, items, and dynamic world events.
*   **Resource system**: Tracks generation, consumption, and limits of core gameplay resources.

## 🟡 Tier 2: Highly Recommended for a "Good" First Impression
*Required to actually test if your combat loop is fun and rewarding.*

*   **Skill system**: Provides abilities, upgrades, and specialization paths for players.
*   **Timer system**: Manages time-based events, cooldowns, resets, and live operations cycles.
*   **Loot / drop system**: Determines rewards, drop rates, rarity, and distribution logic.

## 🛑 Tier 3: Develop Later (Alpha / Beta / Post-Launch)
*The "Meta-game" that gives players a reason to keep grinding matches.*

*   **Inventory system**: Handles item storage, capacity limits, and equipment organization rules.
*   **Progression system**: Defines how players grow, level up, and unlock new power.
*   **Quest system**: Structures player objectives, progression tasks, and mission rewards.
*   **Economy system**: Manages currency flow, pricing, sinks, and player spending behavior.
*   **Social system**: Supports player interaction, cooperation, competition, and group dynamics.

## ❌ Tier 4: Optional / Re-evaluate (Potentially Out of Scope)
*These systems add immense security and technical complexity. Depending on your exact vision for Tomb Tale, strongly consider skipping these to launch faster.*

*   **Trading system**: Enables exchange of items between players. (Creates major security/black-market/RMT risks; usually better to use personal-only loot).
*   **Crafting system**: Combining materials into items. (Distracts from the action combat loop; pure monster-loot drops are easier to balance).
*   **Exploration system**: Encourages discovery of secrets. (Less necessary if your dungeons are meant to be fast, arena-style combat grinds).
