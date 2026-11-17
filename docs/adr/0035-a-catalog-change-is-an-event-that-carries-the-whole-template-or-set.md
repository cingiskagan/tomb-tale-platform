# 35. A catalog change is an event that carries the whole template or set

- **Date:** 2026-11-17
- **Status:** accepted

## Context

An admin can change a template's price or stats, or a set's bonus, and only a log line says
so, which nothing keeps. A fraud case asks who changed a price and when (log policy), and
`events` keeps every domain event forever ([0029](0029-service-audit-inserts-each-event-once-and-keeps-only-listed-fields.md)).

## Decision

We will write an event through inventory's outbox in the transaction of every change:
`item-template.created`, `.updated` and `.retired`, and `item-set.created` and `.updated`,
on `item.events`. Its data is the whole template or set after the change, with the new
`version` and the `updatedBy` who made it. An edit that changes nothing moves no version
and writes no event, so the events of one template are its versions, in order.

## Consequences

- A reader compares two versions to see what changed, since no event stores a diff.
- `updatedBy` is an admin's `publicId`, and `events` keeps it forever.
- A new template field reaches `events` only after its line in `EventAllowList`.
- Audit already takes every routing key of `item.events`, and commerce binds only the
  grant events, so no binding changed.

---

*Implemented by:* `feat: template changes reach the audit store`
