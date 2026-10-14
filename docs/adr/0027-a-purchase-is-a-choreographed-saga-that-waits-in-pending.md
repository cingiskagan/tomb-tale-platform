# 27. A purchase is a choreographed saga that waits in `PENDING`

- **Date:** 2026-10-13
- **Status:** accepted

## Context

A purchase lives in commerce and its items in inventory, so no transaction covers
both ([0022](0022-separate-services-not-a-modular-monolith.md)). Commerce stores any
item code an admin types, and only inventory's catalog knows which codes exist.

## Decision

We will run a purchase as a choreographed saga. Commerce saves it as `PENDING` and
writes `purchase.created` through its outbox. Inventory saves one item grant per
purchase and sends it as `item.granted` or `item.grant-rejected`. Commerce then moves
the purchase to `COMPLETED` or `CANCELLED`, and only these two events move a purchase
out of `PENDING`. Inventory has no outbox. It acknowledges the purchase after the broker
confirms the item grant, so a redelivery sends the saved item grant again.

## Consequences

- `COMPLETED` keeps its meaning: the items are in the player's inventory.
- A purchase whose message reaches a dead-letter queue stays `PENDING` until someone replays that message.
- Commerce can receive an item grant twice, so a repeated one changes nothing.
- The beginner set skips the saga until a grant of its own exists.

---

*Implemented by:* `feat: a purchase grants items across two services`
