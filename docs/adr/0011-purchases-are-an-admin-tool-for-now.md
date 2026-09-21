# 11. Purchases are an admin tool, so `playerId` stays in the request body

- **Date:** 2026-08-12
- **Status:** accepted

## Context

The purchase endpoints needed an authorization policy. The reflex is to take the
buyer from the token, which is right for a player-facing checkout, and there is
none. What exists is an admin screen for creating and correcting purchase rows.

## Decision

We will treat purchases as an admin tool until a player-facing flow exists.
`playerId` stays in `CreatePurchaseRequest`, because the JWT subject says who
acts, not who buys.

| Operation | Allowed |
| --- | --- |
| `POST`, `PUT`, `DELETE` | `platform_admin` only |
| `GET` list, `GET` by id | `platform_admin` or `game_master` |
| anything | never `player` |

## Consequences

- A `platform_admin` can create a purchase against any player id, because
  everyone who reaches these endpoints is already an administrator.
- An `ArgumentCaptor` pins that `playerId` travels unmodified, so a later switch
  to `jwt.getSubject()` fails loudly instead of quietly rewriting who is billed.
- `player` cannot read its own history here. Revisit when a real flow lands.
