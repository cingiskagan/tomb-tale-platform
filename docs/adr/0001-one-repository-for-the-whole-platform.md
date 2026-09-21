# 1. One repository for the whole platform

- **Date:** 2026-03-12
- **Status:** accepted

## Context

The platform is several deployable parts that only work together: two Spring
Boot services, an Angular portal that calls them, and a Compose stack that
serves both. One change often crosses them, and both Java services want the same
Checkstyle and PMD rules. One developer does the work.

## Decision

We will keep everything in one repository: `service-player`,
`service-commerce`, `frontend-portal`, `infrastructure`, the shared rulesets in
`config/`, and the scripts in `scripts/`. Both `pom.xml` files read those
rulesets by relative path.

## Consequences

- A change that crosses a service and the portal is one commit and one pull
  request.
- One copy of each ruleset governs both services, so there is nothing to drift.
- The Compose stack and the code that reads it version together.
- CI runs jobs a change did not touch until the workflows filter by path.
- Releasing one service alone needs tooling we do not have. Today they deploy
  together, which is fine, and it is a real constraint when that stops.
- The repository grows with every service. `service-inventory` and
  `service-dungeon` land here too.
