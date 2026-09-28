# Infrastructure

Copy `.env.example` to `.env`, then run `docker compose up -d` in this directory.

## Routes on the proxy (port 8080)

Traefik sends a request to the matching router with the highest priority.

| Path | Router (priority) | Goes to |
| --- | --- | --- |
| `/` | `zitadel-root-web` (400) | Zitadel login UI |
| `/api/v1/players` | `service-player-local` (300) | service-player on host port 8081 |
| `/api/v1/purchases` | `service-commerce-local` (300) | service-commerce on host port 8082 |
| `/ui/v2/login` | `zitadel-login-web` (250) | Zitadel login UI |
| any other `/api` path | none | 404 from Traefik |
| everything else | `zitadel-canonical-web` (100) | Zitadel API, OIDC and console |

`/api` belongs to the platform services, and no Zitadel router matches it. If
you add a controller under a new prefix, add its router to `traefik-dynamic.yml`.
[ADR 0021](../docs/adr/0021-api-belongs-to-the-platform-not-zitadel.md) records why.

## Ports on the host

| Port | Service |
| --- | --- |
| 8080 | Traefik |
| 8081 | service-player (`run-local.sh`) |
| 8082 | service-commerce (`run-local.sh`) |
| 4200 | frontend-portal (`npm start`) |
| 5432 | Postgres |
| 6379 | Redis |
| 27017 | MongoDB |
| 5672, 15672 | RabbitMQ, and its management UI |
| 1025, 8025 | Mailpit SMTP, and its inbox |
