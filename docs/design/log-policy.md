# Log policy

Read this before Phase I. It decides what the platform records, where each
record lives, and what it never records.

Retention picks the store. A fact the platform must keep goes to the MongoDB
`events` collection with no expiry. Operations output expires, so it stays out
of MongoDB. `service-audit` is the only MongoDB writer
([data store research](data-store-research.md)).

## Never recorded

- Secrets: access and refresh tokens, authorization codes, passwords, reset
  links, client secrets and connection strings ([OWASP](#sources)).
- Request and response bodies, because they carry secrets and personal data.
- Personal data in `events`. The collection never expires and its writer can
  only insert, so an erasure request finds nothing it can delete. Name a person
  by `publicId` only. Account deletion then removes the only link between that
  `publicId` and a person. A name that a player picks is personal data too,
  because it can identify the player with no table, so no event carries one.
  `service-audit` also keeps only the data fields on an allow-list for each
  event type. A field that a producer adds later stays out of `events`.

## Before Phase F

Phase I builds these after Phase E.

- Domain events. `service-audit` stores every published event in `events`,
  with the event id as `_id` (Commit I1).
- Keycloak events. `tombtale-realm.json` sets `eventsEnabled` and
  `adminEventsEnabled`. Keycloak stores both in its own database, and its admin
  console shows them. User events, such as logins and password resets, expire
  after 180 days, past the 120-day chargeback window. Make sure that admin
  events keep no expiry, because a role grant is a security fact.
- `adminEventsDetailsEnabled` stays off. With it on, each admin event copies
  the whole changed object, with its user attributes and client configuration.

## Later

- Gameplay telemetry: dungeon runs, levels, and where players stop. It waits
  for the Unity client and a question someone asks. It gets its own collection
  with a TTL or rollups, and ClickHouse once the volume grows.
- Keycloak events in MongoDB. If the timeline needs logins beside the domain
  events, an event listener (a Keycloak plugin) next to `public-id-mapper`
  publishes them to RabbitMQ. It maps the Keycloak user id to `publicId` and
  drops `username`, which can be an email.
- Admin changes as domain events. `CharacterController` records a stat change
  with `log.info`, and nothing keeps that line. A change by an admin or a game
  master is a fact a fraud case asks about.

## Handed over

Better tools than MongoDB exist for these, and Phase G picks them.

- Container output. Docker's `json-file` driver keeps it, rotated at three
  files of 10 MB, and `docker logs` reads it. A recreate empties it. Phase G
  ships it with Grafana Alloy to Loki, or to the target's own log service.
  Promtail reached end of life on 2026-03-02.
- The nginx access log. It stays in the container output. A 5xx also appears
  as an ERROR in the service log.
- Metrics and traces. Micrometer with OpenTelemetry records both, but sends
  them nowhere yet (ADR 0028). Every service log line carries the trace id,
  and Phase G gives traces and metrics a backend.

## Questionable

The score rates how reasonable each item is, out of 10.

| Item | Score | Reason |
| --- | --- | --- |
| IP address and user agent in Keycloak login events | 8 | An account-takeover case needs them, and the 180-day expiry limits the exposure. |
| Admin reads of player data | 5 | OWASP lists access to sensitive data as an event to record. But every portal list call is a read, and no reader exists yet. |
| Shipped Postgres ERROR lines | 4 | A unique violation prints the value in its DETAIL line, for example a display name. `log_error_verbosity = terse` drops that line. |
| Keycloak events in MongoDB now | 4 | Keycloak already stores them. Only a timeline that joins them to domain events needs the copy. |
| Container WARN and ERROR in MongoDB | 3 | No reader exists on a dev machine, and Loki fits logs better. `events` already gives the document-store practice. |
| Client crash reports in MongoDB | 3 | Anyone can write to the endpoint, and the client sets the timestamp. Sentry has a Unity SDK for this job. |
| nginx 401, 403 and 5xx in MongoDB | 2 | The service log already shows a 5xx, and scanners cause most 401s. |
| Metrics in MongoDB time-series collections | 2 | Prometheus is the tool for this. |
| IP address in `events` | 1 | It is personal data with no expiry and no deletion. |

## Sources

- [OWASP Logging Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html)
- [Keycloak `RealmEventsConfigRepresentation` 26.8.0](https://www.keycloak.org/docs-api/26.8.0/javadocs/org/keycloak/representations/idm/RealmEventsConfigRepresentation.html)
- [Keycloak extensions, including a RabbitMQ event listener](https://www.keycloak.org/extensions)
- [Grafana: Promtail merges into Alloy](https://grafana.com/blog/grafana-loki-3-4-standardized-storage-config-sizing-guidance-and-promtail-merging-into-alloy/)
