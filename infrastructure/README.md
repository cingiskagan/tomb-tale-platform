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

## Postgres

`init-db.sh` gives Keycloak and each service a user and a schema of its own.
Postgres runs the script only on an empty volume. If your volume predates a
user in the script, such as `service_inventory`, the program that needs it
cannot start. Copy any new keys from `.env.example` to `.env`, then run
`docker compose down -v` to start from empty volumes. This deletes all local data.

## MongoDB

`init-mongo.sh` creates the `audit` database and two users
([ADR 0029](../docs/adr/0029-service-audit-inserts-each-event-once-and-keeps-only-listed-fields.md)).
`service_audit` can insert into `events` and do nothing else. `audit_reader`
can read and change nothing. To browse the events in MongoDB Compass, connect
as the reader, with the password from `MONGO_AUDIT_READER_PASSWORD`:

```text
mongodb://audit_reader:<password>@localhost:27017/audit?authSource=audit
```

MongoDB also runs the script only on an empty volume. If your volume predates
the script, stop MongoDB and delete its volume, `tombtale_mongo-data`. This
deletes the stored events. Delete it the same way if `mongo:8.0` created it,
because `mongo:7.0` cannot open it.

## Keycloak

When Keycloak starts, it reads `keycloak/import/tombtale-realm.json`. The file
holds the three roles, `player` as the default role, the portal and game
clients, the `roles` and `public_id` claims, and the login flow: the password,
then a code sent by email. Secrets stay in `.env`, because the file reads `${...}`
placeholders.

`tombtale-game` is the Unity client. It has no secret and no web origin. Its
redirects are fixed in the realm file, because each game build has its own
copy in `PlatformConfig.asset`, and the two must match: `127.0.0.1:7777` for
the editor and desktop, and `com.madnomad.tombtale://` for Android. Its tokens
carry only the `player` and `offline_access` roles, even for staff, because
`fullScopeAllowed` is off and `scopeMappings` names those two. The game asks
for `offline_access`, and an offline session lasts 30 days idle and 90 days at
most ([ADR 0030](../docs/adr/0030-the-game-is-a-public-client-with-offline-sessions-and-touches-only-its-own-data.md)).

Both clients list their own scopes and get lightweight access tokens. Such a
token keeps Keycloak's essentials and only the claims whose mapper sets
`lightweight.claim`: `sub`, `roles` and `public_id`. Names and email stay in
the ID token, which only the portal reads. A new claim the services need must
set it too. A client asking for a scope that is not in its lists gets
`invalid_scope`.

`public_id` is a UUID of its own, never Keycloak's user id. The
`public-id-mapper` module creates it on the user's first token and keeps it as
a user attribute that players can neither see nor edit. The Keycloak image
builds the module from source.

The file also turns on Keycloak events, and the admin console shows them under
Events. User events, such as logins and password resets, expire after 180
days, the 15552000 seconds of `eventsExpiration`. Admin events, such as a role
grant, keep no expiry and store no copy of the changed object. The
[log policy](../docs/design/log-policy.md) gives the reasons.

If the `tombtale` realm already exists, Keycloak does not import the file. After
you change the file, delete the `tombtale` realm in the admin console. This
also deletes its users and its events. Then restart Keycloak:

```bash
docker compose restart keycloak
```

The admin console is at <http://localhost:8180>. Sign in with
`KEYCLOAK_ADMIN_USER` from `.env`. To become a platform admin, register in the
portal, then give your user the `platform_admin` role in the `tombtale` realm.

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

The file holds one policy for each exchange: `player.events`, `purchase.events`
and `item.events`. Each policy sends an event that no queue binds to an
alternate exchange named after the exchange, such as `player.events.unrouted`.
The queue of the same name keeps the event with its routing key. Two services
declare each exchange, so a policy sets this, and no declaration changes. The
service that publishes on an exchange has a test that loads the same file.

## Ports on the host

| Port | Service |
| --- | --- |
| 8080 | nginx |
| 8180 | Keycloak admin console, on localhost only |
| 8081 | service-player (`run-local.sh`) |
| 8082 | service-commerce (`run-local.sh`) |
| 8083 | service-inventory (`run-local.sh`) |
| 8084 | service-audit (`run-local.sh`) |
| 4200 | frontend-portal (`npm start`) |
| 5432 | Postgres |
| 6379 | Redis |
| 27017 | MongoDB |
| 5672, 15672 | RabbitMQ, and its management UI |
| 1025, 8025 | Mailpit SMTP, and its inbox |
