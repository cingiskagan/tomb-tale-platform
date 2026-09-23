# Data store research: inventories, dungeons, and where NoSQL earns its place

Read this before `service-inventory` or `service-dungeon` starts. It records what
shipped games do, so the store choice is made once, against evidence, and then
written into an ADR.

## Current position (2026-09-23)

`service-inventory` and `service-dungeon` both use PostgreSQL.

MongoDB is the one document store, and it holds two collections. `events` keeps
the domain facts published through the outbox, with the event id as `_id` and no
expiry. `logs` keeps application output under a TTL index, so log lines expire
and the record of deeds does not. Fluentd ships the logs through the Docker
`fluentd` log driver, with `fluentd-async` set, because a synchronous driver
blocks a container when Fluentd is down. Metabase draws the dashboards, because
Kibana talks only to Elasticsearch.

The two collections have different recovery paths, and only one of them has a
second copy. Postgres is authoritative for the events, because the outbox rows
stay after publish, so a replay rebuilds `events` in full. Logs have no second
copy: Fluentd is a sink, not a projection, and a lost `logs` collection is gone
apart from whatever the rotated container files still hold. Losing it costs
debugging history and nothing else, which is why the TTL sits there and not on
`events`.

One service owns MongoDB. `service-audit` consumes the events from RabbitMQ,
writes both collections, and serves the timeline endpoint the portal calls. No
other service carries a MongoDB dependency or its credentials, which is the
single-writer rule [ADR 0006](../adr/0006-one-postgres-schema-and-user-per-service.md)
applies to Postgres schemas, kept by hand because MongoDB does not enforce it.
The service starts when the outbox in Commit E1 produces its first event.

The choice is deliberate and it overrides the comparison below. ClickHouse
answers the analytical questions better, and Loki or Elasticsearch handle logs
better. Neither one is a document store, and working with a document store is a
goal of this project.

Revisit when a behavior query takes longer than you accept, or when the log
collection crowds the disk. The event schema is the asset, not the engine, and
append-only data replays into another store in an afternoon.

No ADR records this yet. Write the ADR when the first service starts.

## What shipped games do

| System | Store | What it holds |
| --- | --- | --- |
| TrinityCore (World of Warcraft server) | MySQL | `item_instance` holds one row per stack with a `count` column. `character_inventory` maps that row to a bag and a slot. |
| rAthena (Ragnarok Online server) | MySQL | `inventory` holds `amount`, plus `card0` to `card3` and `option_id0` to `option_id4` as numbered columns. |
| EVE Online | Microsoft SQL Server | Items carry `quantity` and a `singleton` flag. The public engine reference is old, so the schema is the durable part. |
| Nakama (open-source game server) | PostgreSQL or CockroachDB | JSON documents in collections. Its inventory guidance keeps a stack count inside the JSON value. |
| PlayFab Economy v2 | not disclosed | One item id holds several stacks, each with an `amount` and a `StackId`. |
| Halo services | Azure Table Storage | All persistent player state, one partition per player. |
| Fortnite | DynamoDB and RDS | Accounts and in-game purchases. |
| Pokemon GO | Cloud Datastore, then Spanner | Game state moved to Spanner for ACID transactions. Bigtable holds the event log. |

Every published inventory schema above is relational. The document stores appear
either as an API on top of a relational engine, as in Nakama, or as vendor advice
with no named title behind it. TrinityCore shows the cost of the relational
catalog: per-type variation becomes `stat_type1` to `stat_type10`, and the cap
lives in C++ instead of the database. A `jsonb` column removes that cost without
a second store.

## Where NoSQL earns its place

- Player-keyed state at extreme scale, as in Halo and Fortnite. The argument is
  single-key access under millions of concurrent players, not a flexible schema.
- Social graph and messaging. Riot moved chat from MySQL to Riak for friends
  lists, blocked lists and offline messages.
- Telemetry. Pokemon GO writes every action into Bigtable, and Fortnite pushes
  125 million events per minute through Kinesis.
- Derived state. A leaderboard is a Redis sorted set, because `ZREVRANK` reads
  one player's rank from an order the store already maintains, in O(log N).
  MongoDB has `$rank` inside `$setWindowFields` since 5.0, so the operator
  exists. It ranks a sorted partition on each call instead of reading a
  maintained order, which is the wrong cost for a rank that is read constantly
  and updated constantly.

No scale argument above applies to this platform. Data shape is the argument that
does apply.

## Candidate workloads

Three candidates, in the order they earn a store. Each one waits for a named
reader. Without a reader, the store is scaffolding.

### 1. Record of deeds (audit trail)

An append-only history of every player-affecting and system-affecting event, read
by an admin timeline page and by fraud investigation. This is product data with
its own retention, not operations data that expires. Regulated backends state the
same requirement: timestamped, immutable audit trails, queryable for replay.

- Record the actor and the subject apart. A game master granting an item and a
  player earning one are different facts.
- Carry a correlation id across services, so a purchase and an item grant join
  into one story.
- Store the outcome, and for a state change store the value before and after.
- Timestamp on the server. A client timestamp is evidence of nothing.
- Give the writer insert rights only, with no update and no delete.
- Keep the history longer than the chargeback window, which runs past 120 days.
- Keep the outbox rows instead of deleting them after publish. Postgres then
  holds the authoritative sequence, and the document store is a projection that
  a replay rebuilds.
- The consumer writes the document before it acknowledges the message. No queue
  gets a message TTL, and somebody watches the dead-letter queue. A dropped
  message puts a hole in the record where a cheater sits.

### 2. Gameplay statistics

Questions such as "which dungeon types do players repeat", "which builds level
fastest" and "where do players stop playing". This is an analytical workload:
group by, cohorts, funnels and percentiles over one large event table. The games
above answer it in a columnar store, BigQuery for Pokemon GO and Vertica in
Riot's stack, never in the operational database. ClickHouse is the self-hosted
version of that answer. The aggregation pipeline in MongoDB is enough while the
event count stays small.

### 3. Application logs

Not this store. Structured JSON on stdout, plus a log stack such as ELK,
OpenSearch or Loki when search is needed. Logs expire in weeks. A record of deeds
does not.

## Choosing between the stores

DB-Engines put MongoDB fifth overall and Redis seventh in February 2026, so both
carry the same weight as experience. Redis is much lighter, and it is a different
model: sorted sets and streams, not an archive. Elasticsearch answers search and
recent aggregation, and it costs a JVM heap plus three to five times the disk of
a columnar store. ClickHouse suits candidate 2 and stays one binary on one node,
though its own benchmarks are vendor material and it sits outside the DB-Engines
top ten.

## The stacking rule

A row is a stack. It is not one item, and it is not one player and template pair.
An item stacks if and only if no per-copy state tells two copies apart. A healing
potion rolls nothing, so one hundred potions are one row with quantity 100. A
sword rolls modifiers, so each sword is its own row with quantity 1. EVE writes
this same rule as one `singleton` flag.

```sql
CREATE TABLE item_instances (
    id           BIGSERIAL PRIMARY KEY,
    public_id    UUID    NOT NULL UNIQUE,
    owner_id     UUID    NOT NULL,
    template_id  UUID    NOT NULL,
    template_ver INT,
    container    TEXT    NOT NULL,            -- backpack, equipment, bank
    slot         INT     NOT NULL,
    quantity     INT     NOT NULL DEFAULT 1 CHECK (quantity > 0),
    rolled       JSONB,                       -- NULL for a stackable item
    CONSTRAINT uq_item_slot UNIQUE (owner_id, container, slot),
    CONSTRAINT ck_stack_has_no_rolls CHECK (quantity = 1 OR rolled IS NULL)
);
```

On pickup, find a row of the same template with room under the template
`maxStackSize`, increment it, and insert a new row for the remainder. Delete a
row when its quantity reaches zero.

`template_ver` records the version the roll happened against. It is provenance,
not a pin. A read always resolves the current template, which is what lets a
rebalance reach every player with no migration, and it keeps the answer to "why
does this sword have these numbers" auditable. A stackable row rolled nothing,
so it leaves the column null and merges with any other stack of that template.

## Open questions for the first commit

- Which candidate workload gets built first? Candidate 1 needs a timeline
  endpoint, and candidate 2 needs scheduled rollups to stay fast.
- What carries `publicId` and the audit columns in a document? `BaseEntity` needs
  a `Long id`, so ADR 0013 does not reach MongoDB as written.

## Sources

- [TrinityCore `item_instance`](https://trinitycore.atlassian.net/wiki/spaces/tc/pages/2130227/item_instance)
- [rAthena `main.sql`](https://github.com/rathena/rathena/blob/master/sql-files/main.sql)
- [EVE Static Data Export](https://wiki.eveuniversity.org/Static_Data_Export)
- [Nakama storage engine](https://heroiclabs.com/docs/nakama/concepts/storage/)
- [PlayFab Economy v2 inventory stacks](https://learn.microsoft.com/en-us/gaming/playfab/features/economy-v2/inventory/stacks)
- [Riot chat service persistence](https://www.riotgames.com/en/news/chat-service-architecture-persistence)
- [How Pokemon GO scales to millions of requests](https://cloud.google.com/blog/topics/developers-practitioners/how-pok%C3%A9mon-go-scales-millions-requests)
- [How Halo scaled with the saga pattern](https://blog.bytebytego.com/p/how-halo-on-xbox-scaled-to-10-million)
- [AWS and Fortnite case study](https://pages.awscloud.com/fortnite-case-study.html)
- [AWS for Games: choosing the right database](https://aws.amazon.com/blogs/gametech/player-profiles-to-leaderboards-choosing-the-right-aws-database-part-1/)
- [AWS: detecting fraud in games using machine learning](https://aws.amazon.com/blogs/gametech/fraud-detection-for-games-using-machine-learning/)
- [Best practices in casino game backend architecture](https://sdlccorp.com/post/best-practices-in-casino-game-backend-architecture/)
- [DB-Engines ranking](https://db-engines.com/en/ranking)
- [ClickHouse for gaming analytics and telemetry](https://clickhouse.com/industries/gaming)
