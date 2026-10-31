# Game server

Read this before the first network code in the Unity project. It records the
multiplayer technology chosen on 2026-10-31, and how the game server uses it.

## Requirements

- A town holds 20 players, and a dungeon holds one party of 5.
- Fewer than 50 players are online at first, because the game is not
  marketed. The design must scale if more players come.
- The targets are the Play Store and Steam. No browser build and no PvP are
  planned.
- The least build effort that still keeps item, currency and progress grants
  on a server ([ADR 0030](../adr/0030-the-game-is-a-public-client-with-offline-sessions-and-touches-only-its-own-data.md)).

## Decision

We use FishNet, the free version, in a Unity dedicated server build of the
game project. The server reuses the game's physics, NavMesh and room prefabs,
so no simulation code is written twice. One process holds every town and every
dungeon, and it runs as a container beside the platform.

## One process, many instances

- A town is a scene. FishNet stacks a new copy when a copy holds 20 players.
- A dungeon is an empty scene, stacked once per party, that the server fills
  from the layout. FishNet loads the copy only for the party's connections. A
  player sees only the objects in that player's scenes. The copy unloads when
  the last player leaves.
- Each dungeon sits at its own world position, for example 1 km apart. Every
  dungeon has its own NavMesh, but Unity keeps all loaded NavMeshes in one
  navigation world and searches them by position. In overlapping dungeons, an
  enemy could land on another dungeon's mesh and follow its corridors.
  Separate positions also keep the physics apart, so the stacked scenes need
  no local physics.
- `service-dungeon` makes the layout. The server sends the party the layout,
  never the seed, because Java and C# use different random generators: one
  seed would build two different dungeons.
- Walls and floors are not network objects. Each side builds them from the
  layout, and only players, enemies, doors and loot cross the network.
- Only the server needs a NavMesh. Each dungeon bakes its own from colliders,
  because a server build drops the vertex data of meshes that are only drawn,
  so a bake from render meshes finds nothing.
- The NavMeshSurface sits on the dungeon's root and collects only its
  children. By default it collects every loaded scene, so it would also cover
  the other dungeons and the towns, and lay a second mesh over theirs.
- The bake runs on worker threads through `UpdateNavMesh`, and only collecting
  the children runs on the main thread. `BuildNavMesh` blocks the main thread,
  so it would freeze every instance in the process.
- Enemies spawn after the bake finishes, because an agent needs a NavMesh to
  stand on. When the dungeon unloads, its surface removes its NavMesh.

## One session per player

A player is in one place at a time. A second login replaces the first
connection and puts the player where the first one was: in the same dungeon
copy if it still runs, otherwise in a town.

One process enforces this from its own connections. With several processes, a
Redis key per `public_id` holds the player's place. The allocator writes it,
and it expires if the server dies.

## Authority

The client moves its own character, and the server rejects impossible moves,
such as too much speed or a path through a wall. Skills, damage, AI and loot
run on the server. Server-owned movement needs prediction, the hardest part of
network code, and PvE gains little from it. If PvP comes, movement moves to the server with
FishNet's prediction, which is free.

A server build keeps running Animators. Enemy damage comes from the sword
animation today (`SwordHitbox`), so hit timing must move into data, in
seconds, before the server can drop Animators.

## Item grants

The server grants each drop to inventory when it drops, not at the end of the
run, so a crash loses only the grants still in flight. Each drop carries a
`dropId` that the server makes, and a retry with the same `dropId` grants
nothing twice. Inventory stores the `dropId` under a unique constraint, as it
stores a purchase's `purchase_id` in `item_grants`, and answers a repeat with
the first result.

The server calls inventory as its own Keycloak client, with a `game_server`
role that no player token carries. Inventory still checks that the item
exists and the quantity is sane.

## Transport

FishNet's Unity Transport adapter carries Android and Steam players over one
UDP transport, so they can share a town. The client sends its Keycloak access
token when it connects, so we turn on the adapter's DTLS encryption, which is
off by default. Tugboat, FishNet's default transport, has no encryption.
Steam's own networking is out, because Android players have no Steam.

## Scaling

1. Now: one process.
2. Hundreds of players: more processes, each a channel with its own towns and
   dungeons, so a party stays inside one process. An allocator, the TDD's
   matchmaker, places each party on a process with room and gives the client
   its address. Redis holds the channel list and the load. Party and chat
   reach across processes over RabbitMQ.
3. Beyond that: dungeons in short-lived processes, started by Agones or
   Edgegap.

The game server keeps no permanent data, so any process can take any player.
Unity's Multiplay hosting takes no new servers or allocations since
2026-04-01, and Hathora closed on 2026-05-05, so the server stays a plain
container that any host can run.

## Rejected

- Netcode for GameObjects syncs the server's scenes to every client, so an
  isolated dungeon needs a process of its own.
- Mirror is the close second and the fallback. It is MIT licensed and has free
  lag compensation, but per-party scene copies are example code we would
  adapt, and its prediction is experimental and covers only rigidbodies.
- Photon Fusion charges per online player and is closed source, and it still
  needs our own servers for authority.
- Nakama, Colyseus or a server of our own run outside Unity. Physics, the
  NavMesh and the room data would be rebuilt, and the network layer written on
  both sides.

## Limits and open questions

- A process has one main thread, so a heavy dungeon slows down its whole
  process. We scale with more processes, not bigger ones.
- FishNet has one main developer and a custom license, which is free and
  royalty-free for games. Versions break the API, so we pin the version.
- Pro costs $50 once, and nothing in this plan needs it. Server code stays out
  of the client through `#if UNITY_SERVER || UNITY_EDITOR`.
- A crash ends every town and dungeon in the process. Loot already granted
  stays, and the players come back in a town. A dungeon also unloads when its
  last player leaves, so a solo player whose phone crashes comes back in a
  town. Whether a dungeon waits for a reconnect is open.
- Whether grants travel over REST or RabbitMQ is open. The server has no
  database, so it has no outbox, and an unsent grant lives only in memory.
- Nothing decides yet how a new server version meets the older clients that
  the Play Store still has installed.
- Enemies must work when spawned at runtime. Today `EnemyAI` reads a player
  reference that is set in the scene.
- The town step is the first test of FishNet with real players, and it
  measures the server's memory before the dungeon builds on it.

## Sources

- [FishNet: scene stacking](https://fish-networking.gitbook.io/docs/guides/features/scene-management/scene-stacking)
- [FishNet: Pro features and prices](https://fish-networking.gitbook.io/docs/overview/readme/pro-projects-and-support)
- [FishNet: Unity Transport adapter](https://fish-networking.gitbook.io/docs/fishnet-building-blocks/transports/fishyunitytransport)
- [FishNet license](https://github.com/FirstGearGames/FishNet/blob/main/LICENSE.md)
- [Mirror: Multiple Additive Scenes example](https://mirror-networking.gitbook.io/docs/manual/examples/multiple-additive-scenes)
- [Unity: Dedicated Server optimizations](https://docs.unity.com/en-us/engine/current/manual/platform-specific/dedicated-server/get-started/optimizations)
- [Unity: NavMesh with additive scenes](https://docs.unity3d.com/cn/2022.3/Manual/nav-AdditiveLoading.html)
- [Unity: NavMeshSurface API](https://docs.unity3d.com/Packages/com.unity.ai.navigation@2.0/api/Unity.AI.Navigation.NavMeshSurface.html)
- [Unity: Multiplay deprecation notice](https://status.unity.com/info_notices/362941)
