---
description: How the client, the game server and the platform work together in Tier 1.
---

# Tomb Tale Online RPG: Tier 1 Technical Design Document

The [GDD](tier-1-game-design-document.md) says what happens in Tier 1. This document says which piece does what, and what crosses between them. [Game server](game-server.md) records the network technology and why it was chosen.

## 1. The Three Pieces

1. **Game client:** Unity, on Android through the Play Store and on desktop through Steam. It logs in with Keycloak ([ADR 0030](../adr/0030-the-game-is-a-public-client-with-offline-sessions-and-touches-only-its-own-data.md)), calls the platform through nginx, and connects to the game server. It owns its character's movement and nothing else.
2. **Game server:** a Unity dedicated server build with FishNet. One process runs every town and dungeon copy, decides combat, AI and drops, and keeps no permanent data.
3. **Platform:** the Spring Boot services behind nginx, with Keycloak, Postgres, RabbitMQ and MongoDB. It keeps everything permanent.

## 2. Town Step

### Connecting

- The client sends its Keycloak access token through FishNet's authenticator. The server checks the signature against Keycloak's published keys, the issuer and the expiry, and reads `public_id`. Any other token closes the connection.
- A player is in one place at a time. A second connection with the same `public_id` replaces the first, and the player goes where the first one was: the same dungeon copy if it still runs, otherwise a town ([game server](game-server.md#one-session-per-player)).
- The server puts the player in a town copy with room, and opens a new copy when every copy is full.

### Names

- The game server is a Keycloak client of its own. It gets its token through the client credentials grant, and that token carries a `game_server` role that no player token carries.
- On connect, the server reads the player's character from `service-player` with that token and shows the character's name. A player has one character for now, so there is nothing to choose.
- `service-player` needs a read of a player's characters that only `game_server` may call.

### Movement

The client moves its character, and FishNet sends the position to the other players in the copy. The server rejects impossible moves ([game server](game-server.md#authority)).

### Running It in Development

- The server runs on the dev machine, next to the platform.
- A phone reaches the game server over the local network, because `adb reverse` forwards TCP only and the game traffic is UDP. Keycloak stays behind `adb reverse tcp:8080`.
- DTLS needs a certificate. The server may run without DTLS on the local network, and it runs with DTLS before it leaves the dev machine.

### Done When

- A phone and a desktop build see each other move in one town copy, each with its character's name.
- With the town cap set to 2, a third player opens a second copy and sees neither of the first two.
- The server's memory is measured with those players connected.

## 3. Dungeon Step

- **No matchmaker yet:** the game server moves a party from its town copy into a new dungeon copy inside its own process. An allocator comes with a second process ([game server](game-server.md#scaling)).
- **Run id:** each run gets a `runId`, made by the game server while it runs alone and by the allocator after that.
- **Stats:** before the run, the server reads each member's character from `service-player` and their equipment from `service-inventory`. When a player changes equipment during the run, the client tells the server, and the server reads the new equipment from inventory instead of trusting the client. The schema for combat stats is not defined yet.
- **Layout:** the server loads the fixed layout first, and later asks `service-dungeon` for one. It sends the layout to the party.
- **Drops:** the server grants each drop to inventory when it falls, with a `dropId` ([game server](game-server.md#item-grants)).
- **End:** a win or a loss returns the party to the town. The server reports the run with its `runId`, so a repeated report counts once, and `service-player` keeps the count of runs played. Whether grants and reports travel over REST or RabbitMQ is open.

## 4. Security Principles

- **Never trust the client, except for movement:** the client owns its position, and the server checks it. A client only asks to cast a skill. The server checks stamina and cooldown, resolves the hit, and tells every client.
- **Only the game server grants:** items and run reports come from the `game_server` role. A game token reaches only the player's own data ([ADR 0030](../adr/0030-the-game-is-a-public-client-with-offline-sessions-and-touches-only-its-own-data.md)).
- **Payload validation:** the platform validates every request from the game server like any other request, with typed DTOs and Jakarta validation.
