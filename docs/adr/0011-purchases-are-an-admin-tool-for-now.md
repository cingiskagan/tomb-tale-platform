# 11. Purchases are an admin tool, so `playerId` stays in the request body

- **Date:** 2026-08-12
- **Status:** accepted

## Context

The purchase endpoints needed an authorization policy, and the obvious one
looked wrong on inspection.

The reflex for a purchase API is to take the buyer from the token:
`playerId = jwt.getSubject()`, so a caller can only buy for themselves. That is
correct for a player-facing checkout, and there is no player-facing checkout.
Nothing in the portal lets a player buy anything. What exists is an admin screen
used to create and correct purchase rows while the economy is being built.

Taking the buyer from the token would therefore make the subject the actor, not
the buyer, and would break the only workflow the endpoints actually have: an
administrator creating a purchase on behalf of a player.

## Decision

We will treat purchases as an administrative tool until a real player-facing
purchase flow exists.

`playerId` stays in `CreatePurchaseRequest`. The JWT subject identifies who is
acting, not who is buying.

The access matrix is:

| Operation | Allowed |
| --- | --- |
| `POST`, `PUT`, `DELETE` | `platform_admin` only |
| `GET` list, `GET` by id | `platform_admin` or `game_master` |
| anything | never `player` |

An `ArgumentCaptor` in the controller tests pins that `playerId` travels from
request body to controller to service unmodified.

## Consequences

- The endpoints match the only workflow that exists, instead of a checkout that
  does not.
- A caller with `platform_admin` can create a purchase against any player id. The
  policy is deliberately trusting, because everyone who can reach these endpoints
  is already an administrator.
- The `ArgumentCaptor` assertion is the guard on this decision. When somebody
  later switches to `jwt.getSubject()`, that test fails loudly and points at this
  record, rather than the change landing silently and quietly rewriting who gets
  billed.
- `player` has no access to its own purchase history through this API. That is
  acceptable only while there is nothing for a player to buy, and it is the first
  thing to revisit when a player-facing flow lands.
- Revisit this record when that flow arrives. The likely outcome is a second,
  separate endpoint for player self-service rather than a change to these ones.
