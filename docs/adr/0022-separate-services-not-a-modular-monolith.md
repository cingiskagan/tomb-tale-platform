# 22. Separate services, not a modular monolith

- **Date:** 2026-09-28
- **Status:** accepted

## Context

service-player and service-commerce run as two applications. The split dates
from 2026-03-17, and nobody wrote down why. The architecture review of
2026-07-10 found each service to be plain CRUD over one or two tables. It said
that a modular monolith does the same job at half the operating cost.
`docs/design/pathway.md` plans five services.

## Decision

We will keep one Spring Boot application per service in `pathway.md`. Services
share no tables ([0006](0006-one-postgres-schema-and-user-per-service.md)) and
talk through RabbitMQ events or HTTP. There are two reasons: the roadmap, and
practice with the problems that a monolith hides.

## Consequences

- A write that crosses services is a dual write. `player.created` needs a
  transactional outbox, and every consumer must be idempotent.
- A purchase that grants an item needs a saga with compensation, because no
  transaction spans two databases.
- Each service carries its own Flyway history, CI job and Traefik router.
- Until a third service lands, the split costs more than it returns. If the
  roadmap drops to two services, merge them and supersede this record.
