# 30. The game is a public client with offline sessions, and touches only its own data

- **Date:** 2026-10-27
- **Status:** accepted

## Context

The Unity client runs on the player's device. It has no web origin, cannot keep a secret, and
anything it sends can be forged. A normal Keycloak session ends after 30 minutes idle, so a
player back from a break would type the password and the email code again.

## Decision

We will register `tombtale-game` as a public client with PKCE S256 and no web origin. It logs in
through the system browser, a Custom Tab on Android and never a webview, and asks for `offline_access`:
an offline session lasts 30 days idle and 90 days at most, and the access token keeps Keycloak's 5 minutes.
The client's role scope is only `player` and `offline_access`, so its token acts on the player's own data
alone, even for staff. Granting items, currency or progress, or reporting a match result, waits for a game server.

## Consequences

- A player stays logged in across restarts, and types the password and code once in 90 days.
- The refresh token is a 30-day credential, kept in the Android Keystore. `POST /logout` ends it at once, and also a portal session that shares the browser (tested 2026-10-27).
- After an offline login Keycloak keeps no browser session, so a login after a logout asks for the password and code.
- An access token outlives a ban or a role change by up to 5 minutes.
- A game token reaches only `POST` and `PATCH /players/me`. Staff use the portal, and character stats stay GM-only until a game server exists.

---

*Implemented by:* `feat(keycloak): the realm registers the Unity client`
