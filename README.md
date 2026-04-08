# Tomb Tale Platform

## Overview
Tomb Tale Platform is the monorepo for the "Tomb Tale Online RPG" services. This repository contains the platform microservices, the frontend web portal, and the shared infrastructure dependencies required to run the game ecosystem locally.

### Project Structure
- `frontend-portal/`: The Angular-based web portal for users.
- `service-commerce/`: Spring Boot Java microservice handling purchases, payments, and economy.
- `service-player/`: Spring Boot Java microservice handling player account management.
- `infrastructure/`: Docker Compose configurations and initialization scripts for local databases and auth providers.
- `scripts/`: Shared platform automation and CI/CD scripts.

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
#not implemented yet
```

### 3. Start Frontend Portal
To run the Angular frontend portal:

```bash
cd frontend-portal
npm install
npm start
```

The frontend application will start up and be accessible locally at `http://localhost:4200`.

## Contributor Guidelines
- Run linting and automated tests locally before opening a Pull Request.
- Follow the [Angular Style Guide](https://angular.io/guide/styleguide) for `frontend-portal`.
- Strict typing and explicit formatting are mandatory across all tiers of the stack.
