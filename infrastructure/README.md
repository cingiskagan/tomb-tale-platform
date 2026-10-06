# Infrastructure

Copy `.env.example` to `.env`, then run `docker compose up -d` in this directory.

## Routes on the proxy (port 8080)

nginx sends a request to the `location` in `nginx.conf` that matches it.

| Path | Goes to |
| --- | --- |
| `/api/v1/players` | service-player on host port 8081 |
| `/api/v1/purchases` | service-commerce on host port 8082 |
| `/realms/`, `/resources/` | Keycloak login pages and OIDC endpoints |
| any other path | 404 from nginx |

`/api` belongs to the platform services. If you add a controller under a new
prefix, add its `location` to `nginx.conf`, or the prefix answers 404.
[ADR 0025](../docs/adr/0025-nginx-replaces-traefik.md) records why.

## Keycloak

Keycloak reads `keycloak/import/tombtale-realm.json` when it starts. The file
holds the three roles, `player` as the default role, the portal client, the
`roles` and `public_id` claims, and the login flow: the password, then a code
sent by email. Secrets stay in `.env`, because the file reads `${...}`
placeholders.

`public_id` is a UUID of its own, never Keycloak's user id. The
`public-id-mapper` module creates it on the user's first token and keeps it as
a user attribute that players can neither see nor edit. The Keycloak image
builds the module from source.

Keycloak imports a realm only when the realm does not exist yet. After you
change the file, delete the `tombtale` realm in the admin console, then restart
Keycloak:

```bash
docker compose restart keycloak
```

The admin console is at <http://localhost:8180>. Sign in with
`KEYCLOAK_ADMIN_USER` from `.env`. To become a platform admin, register in the
portal, then give your user the `platform_admin` role in the `tombtale` realm.

Postgres runs `init-db.sh` only on an empty volume. If your volume predates
Keycloak, it has no `keycloak` user, and Keycloak cannot start. Run
`docker compose down -v` to start from empty volumes. This deletes all local data.

## RabbitMQ definitions

`rabbitmq/definitions.json` holds the broker objects that no service owns. A
`post_start` hook on the `rabbitmq` service loads it with `rabbitmqctl
import_definitions` once the broker runs. If the broker loads definitions at
boot, it skips its default user, so the file goes in after startup.

Compose runs the hook only as it creates the container. The broker keeps what
it loaded in its volume, so a restart loses nothing. After you change the file,
load it into the running broker:

```bash
docker compose exec -u rabbitmq rabbitmq rabbitmqctl import_definitions /etc/rabbitmq/platform/definitions.json
```

An import adds and updates objects, but it never deletes one. If you remove an
object from the file, also delete it in the management UI. Make every change in
the file first. A change made only in the UI stays in your volume and reaches
no one else.

Today the file holds one policy. `player.events` sends an event that no queue
binds to the alternate exchange `player.events.unrouted`. The queue of the same
name keeps the event with its routing key. Both services declare
`player.events`, so the policy sets this, and neither declaration changes.
`PlayerEventsAlternateExchangeTest` in service-player loads the same file.

## Ports on the host

| Port | Service |
| --- | --- |
| 8080 | nginx |
| 8180 | Keycloak admin console, on localhost only |
| 8081 | service-player (`run-local.sh`) |
| 8082 | service-commerce (`run-local.sh`) |
| 4200 | frontend-portal (`npm start`) |
| 5432 | Postgres |
| 6379 | Redis |
| 27017 | MongoDB |
| 5672, 15672 | RabbitMQ, and its management UI |
| 1025, 8025 | Mailpit SMTP, and its inbox |
