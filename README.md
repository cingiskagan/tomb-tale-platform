# Tomb Tale Platform

## Overview

Tomb Tale Platform is the monorepo for the "Tomb Tale Online RPG" services. This repository contains the platform microservices, the frontend web portal, and the shared infrastructure dependencies required to run the game ecosystem locally.

### Project Structure

- `frontend-portal/`: The Angular-based web portal for users.
- `service-commerce/`: Spring Boot Java microservice handling purchases, payments, and economy.
- `service-player/`: Spring Boot Java microservice handling player account management.
- `infrastructure/`: Docker Compose configurations and initialization scripts for local databases and auth providers.
- `scripts/`: Shared platform automation and CI/CD scripts.

## Project Status

Development is platform-first: the backend services and the Angular admin portal
come before any Unity game client. Nothing is deployed anywhere yet — everything
below runs locally against the Docker Compose stack.

| Component | State | What works today |
| --- | --- | --- |
| `service-player` | Partial | Read and update your own profile; list players with filtering, sorting and paging (admin and game master); update a character's stats with an ownership check. Flyway-managed schema, 41 tests. |
| `service-commerce` | Working | Create, read, list, update and cancel purchases, with a purchase status state machine. Mutations are admin-only, reads are open to game masters too. Flyway-managed schema, 64 tests. |
| `frontend-portal` | Working | Zitadel login and callback, dashboard, own-profile page, and role-gated player and purchase lists. |
| `infrastructure` | Working | Traefik, Zitadel, Postgres, Redis, MongoDB and RabbitMQ, started with one `docker compose up`. |
| `service-inventory` | Planned | See the [development pathway](docs/design/pathway.md). |
| `service-dungeon` | Planned | See the [development pathway](docs/design/pathway.md). |
| Unity game client | Planned | Deliberately last. |

Some of the stack is wired but not yet earning its place. RabbitMQ is configured
in both services, and neither publishes or consumes a message. Redis and MongoDB
run in the Compose stack for services that will need them; no service connects to
either today.

Characters are read and stat updates only — there is no endpoint yet to create or
delete one.

## Local Development Guide

### 1. Start Local Infrastructure

Before running any application code, start the necessary infrastructure dependencies (PostgreSQL, RabbitMQ, MongoDB, Zitadel) via Docker Compose.

```bash
cd infrastructure
docker compose up -d
```

*(Ensure all containers are running and healthy before proceeding).*

### 2. Run Backend Services

Navigate to the module directory for any of the Spring Boot microservices to start them. The services use Maven and are configured to connect to the local Docker infrastructure by default.

**Service Commerce:**

```bash
cd service-commerce
./run-local.sh
```

**Service Player:**

```bash
cd service-player
./run-local.sh
```

### 3. Start Frontend Portal

To run the Angular frontend portal:

```bash
cd frontend-portal
npm install
npm start
```

The frontend application will start up and be accessible locally at `http://localhost:4200`.

## Documentation

- [`docs/adr/`](docs/adr/README.md) — Architecture Decision Records: why the
  platform is built the way it is, one decision per file, in the order they were
  made.
- [`docs/design/`](docs/design/) — game and technical design documents, and the
  development pathway.

## Contributor Guidelines

- Run linting and automated tests locally before opening a Pull Request.
- Follow the [Angular Style Guide](https://angular.io/guide/styleguide) for `frontend-portal`.
- Strict typing and explicit formatting are mandatory across all tiers of the stack.
