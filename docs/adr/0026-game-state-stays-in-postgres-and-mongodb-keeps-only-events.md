# 26. Game state stays in Postgres, and MongoDB keeps only `events`

- **Date:** 2026-10-11
- **Status:** accepted

## Context

The pathway first put service-inventory and service-dungeon on MongoDB, for
item stats that vary per item type. The [data store research](../design/data-store-research.md)
found every published inventory schema relational. Practice with a document
store is still a goal of this project.

## Decision

We will keep game state in Postgres, one schema per service as in
[0006](0006-one-postgres-schema-and-user-per-service.md), with `jsonb` for what
varies per item type. MongoDB keeps one collection, `events`, with no expiry,
and `service-audit` is its only writer. Logs stay out ([log policy](../design/log-policy.md)).

## Consequences

- Three services and Keycloak share one Postgres instance, its connection
  limit and its outages.
- `jsonb` has no column constraints, so a service validates a stat pool itself.
- MongoDB idles until `service-audit` exists, and only a review keeps its
  credentials out of the other services.

---

*Implemented by:* `feat(inventory): service-inventory owns the item catalog`
