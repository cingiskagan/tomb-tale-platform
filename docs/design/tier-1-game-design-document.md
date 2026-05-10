---
title: Tomb Tale Online RPG - Tier 1 Game Design Document
description: Initial Game Design Document focusing on the MVP Tier 1 combat loop.
---

# 📜 Tomb Tale Online RPG: Tier 1 Game Design Document

## 1. Executive Summary
The objective of the **Tier 1 MVP** is to create a fully playable, networked proof-of-concept for the core combat loop of "Tomb Tale." The scope is strictly limited to a single dungeon room where a player can connect, encounter an enemy, and successfully resolve combat to a win/loss state.

All meta-game systems (Inventory, Economy, Questing, Progression) are explicitly out of scope for this prototype to ensure focus on combat "feel" and network stability.

---

## 2. Infrastructure Assumptions (Prerequisites)
To test this Tier 1 design, the underlying architecture must support:
*   **Networking**: A dedicated server instance running the combat logic authoritatively. 
*   **Matchmaking**: A barebones launcher or lobby that connects 1-4 players into the same dungeon instance room.
*   **Persistence**: A basic player profile pulling an initial set of stats (e.g., Base HP, Base Damage) from the `service-player` database.

---

## 3. Tier 1 Systems Breakdown

### 3.1 Resource System
The Resource System handles numerical pools that gate character existence and actions.

*   **Health Points (HP)**
    *   **Generation**: None in combat for MVP (Healing potions are Tier 2).
    *   **Consumption**: Reduced when hit by an enemy attack.
    *   **Limits**: Capped at `MaxHP`. If `CurrentHP <= 0`, the entity enters the `[Death]` state.
*   **Stamina / Action Points (AP)**
    *   **Generation**: Passive regeneration per server tick (e.g., +5 Stamina per second).
    *   **Consumption**: Executing an attack costs a fixed amount of Stamina.
    *   **Limits**: Capped at `MaxStamina`. Cannot initiate an attack if `CurrentStamina < AttackCost`.

### 3.2 Spawning System
Controls the lifecycle of entities within the dungeon instance.

*   **Instance Initialization**: Upon instance creation, the server loads a predefined "Dungeon Room" layout.
*   **Player Spawns**: Players spawn at designated, safe target points (e.g., `Point_PlayerSpawn_1`). If HP reaches 0, they despawn and the match is lost.
*   **Enemy Spawns**: Enemies are placed at predefined coordinates. At MVP, there are no complex "wave managers"—all enemies spawn immediately upon instance initialization.

### 3.3 AI Behavior System
All NPCs (monsters) use a simplified Finite State Machine (FSM) executed on the authoritative server.

*   **State 1: Idle**: Enemy stands at spawn point. Looks for targets within an Aggro Radius.
*   **State 2: Chase**: If a player enters the Aggro Radius, the enemy paths directly toward the player.
*   **State 3: Attack**: If the player is within the enemy's `AttackRange`, standard movement stops, and the monster executes an attack animation/event.
*   **State 4: Death**: Triggered when `HP <= 0`. Plays death animation, disables collision, and despawns after a set timer.

### 3.4 Combat System
The core collision and mathematics engine for resolving fights.

*   **Hit Detection**: 
    *   Uses simple collision forms (Server-side physics boxes or capsules).
    *   When an attacker executes a "Strike" event, it generates a hitbox in front of them for a specific duration (Active Frames). 
    *   If a defender's hurtbox intersects the hitbox, a hit is registered.
*   **Damage Calculation (MVP Formula)**:
    *   `Final Damage = Max(1, (AttackerBaseDamage * AttackMultiplier) - DefenderArmor)`
*   **Hit Reactions**:
    *   Applying damage must interrupt the defender's movement slightly (Hit Stun) to provide physical feedback to the players.
*   **Match Resolution**:
    *   **Win**: All AI entities in the Spawning System reach the `[Death]` state. Triggers transition back to lobby.
    *   **Loss**: All Player entities reach the `[Death]` state. Triggers transition back to lobby.

---
*End of Tier 1 GDD.*
