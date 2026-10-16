# 28. Micrometer with OpenTelemetry traces the services, and exports nothing yet

- **Date:** 2026-10-16
- **Status:** accepted

## Context

A purchase crosses commerce, RabbitMQ and inventory, and the outbox sends its event
later, in a scheduled job ([0027](0027-a-purchase-is-a-choreographed-saga-that-waits-in-pending.md)).
A stuck purchase leaves lines in three logs, and nothing joins them. Phase G picks a
backend for traces and metrics. The `otel-collector` in compose came with Zitadel.

## Decision

We will trace with Micrometer Tracing over OpenTelemetry, through
`spring-boot-starter-opentelemetry`. Every request is sampled, and nothing is exported:
no OTLP endpoint, and OTLP metrics off. RabbitMQ observation carries the W3C `traceparent`
in each message. The outbox row stores the `traceparent` of its writer, and the publisher
sends the row inside that trace. The collector leaves compose.

## Consequences

- A log line written inside a request or a message carries the trace id, in every service.
- A backend needs an OTLP endpoint and no code, because the services speak only OTLP.
- With trace export off, Spring Boot propagates no trace. Tests turn it off, so a trace test needs `@AutoConfigureTracing`.
- Metrics stay at `/actuator/metrics` until Phase G.

---

*Implemented by:* `feat: one trace id follows an event across the services`
