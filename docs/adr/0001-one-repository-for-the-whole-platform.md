# 1. One repository for the whole platform

- **Date:** 2026-03-12
- **Status:** accepted

## Context

The platform is several deployable pieces that only make sense together: two
Spring Boot services, an Angular portal that calls them, and a Docker Compose
stack that provides Postgres, Redis, MongoDB, RabbitMQ, Traefik and Zitadel for
all of them. `docs/design/pathway.md` plans two more services after these.

A single change often crosses those boundaries. Adding a role-gated endpoint
touches a service and the portal. Changing a port touches a service, the compose
file and the portal's environment file. Both Java services want the same
Checkstyle and PMD rules, and there is no reason for those rules to differ.

The work is done by one developer. Split across repositories, one change becomes
several pull requests that have to land in order, and the shared analysis rules
become copies that drift.

## Decision

We will keep everything in one repository: `service-player`, `service-commerce`,
`frontend-portal`, `infrastructure`, the shared rulesets in `config/`, and the
scripts in `scripts/`.

The Checkstyle and PMD rulesets live once, in `config/checkstyle/` and
`config/pmd/`, and both `pom.xml` files reference them by relative path.

## Consequences

- A change that crosses a service and the portal is one commit and one pull
  request, reviewed as one thing.
- One copy of each ruleset governs both services. There is no drift to
  reconcile, because there is nothing to drift from.
- The compose stack and the code that depends on it version together.
  `infrastructure/.env.example` cannot fall out of step with the
  `application.yml` files that read those variables.
- CI runs work that a change did not touch unless the workflows filter by path.
  A frontend-only change can still trigger Java jobs.
- Services cannot be versioned or released independently without extra tooling.
  Today they deploy together, which is fine, and it is a real constraint the
  moment that stops being fine.
- The repository grows with every new service. `service-inventory` and
  `service-dungeon` will land here too.
