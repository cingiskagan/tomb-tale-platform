---
description: Game design for Tier 1, the first playable game, a town and then a dungeon run.
---

# Tomb Tale Online RPG: Tier 1 Game Design Document

Tier 1 is the first playable game. Players meet in a town, form a party of up to 5, and clear a dungeon. It is built in two steps, the town and then the dungeon ([pathway](pathway.md)).

Skills beyond the basic attack, drop tables, progression, quests, the economy and trading come in later tiers.

## 1. Town

- A town copy holds 20 players. When every copy is full, a new copy opens.
- A player who logs in appears at the town's spawn point as their character, with the character's name over its head.
- Players move with the touch joystick on Android and the keyboard on Steam, and see every other player in their copy move.
- The town is safe: it has no enemies and no combat.
- A player who leaves the game leaves the town.

## 2. Dungeon

### 2.1 The Run

- A party of 1 to 5 players leaves the town for a dungeon copy of its own. How a party forms is open.
- The first dungeon has one fixed layout. Later, every run gets a generated layout of rooms and corridors.
- **Win:** every enemy is dead. **Loss:** every player is dead. Either way, the party returns to the town.
- A drop goes into the player's inventory when it falls.
- A player who logs in again, on any device, returns to the dungeon if it still runs, otherwise to the town.

### 2.2 Resource System

The Resource System handles numerical pools that gate character existence and actions.

- **Health Points (HP)**
  - **Generation**: None in combat for MVP (Healing potions are Tier 2).
  - **Consumption**: Reduced when hit by an enemy attack.
  - **Limits**: Capped at `MaxHP`. If `CurrentHP <= 0`, the entity enters the `[Death]` state.
- **Stamina / Action Points (AP)**
  - **Generation**: Passive regeneration per server tick (e.g., +5 Stamina per second).
  - **Consumption**: Executing an attack costs a fixed amount of Stamina.
  - **Limits**: Capped at `MaxStamina`. Cannot initiate an attack if `CurrentStamina < AttackCost`.
  - **Name**: a mage's pool is called Mana. Only the label changes, and the pool works the same.

### 2.3 Spawning System

- **Players** spawn at the layout's player spawn points. A player at 0 HP despawns until the run ends.
- **Enemies** spawn at the layout's enemy spawn points, all at once when the run starts. There is no wave manager yet.

### 2.4 AI Behavior System

All NPCs (monsters) use a simplified Finite State Machine (FSM) executed on the authoritative server.

- **State 1: Idle**: Enemy stands at spawn point. Looks for targets within an Aggro Radius.
- **State 2: Chase**: If a player enters the Aggro Radius, the enemy paths directly toward the player.
- **State 3: Attack**: If the player is within the enemy's `AttackRange`, standard movement stops, and the monster executes an attack.
- **State 4: Death**: Triggered when `HP <= 0`. Plays death animation, disables collision, and despawns after a set timer.

### 2.5 Combat System

- **Hit Detection**:
  - Uses simple collision forms (server-side boxes or capsules).
  - A strike opens a hitbox in front of the attacker for an active window, set in seconds rather than by the animation.
  - If a defender's hurtbox intersects the hitbox, a hit is registered.
- **Damage Calculation (MVP Formula)**:
  - `Final Damage = Max(1, (AttackerBaseDamage * AttackMultiplier) - DefenderArmor)`
- **Hit Reactions**:
  - Applying damage must interrupt the defender's movement slightly (Hit Stun) to provide physical feedback to the players.
