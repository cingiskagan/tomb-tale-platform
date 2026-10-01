# 23. The player's `publicId` travels in the access token

- **Date:** 2026-10-01
- **Status:** accepted

## Context

Every service needs the caller's `publicId`, for `createdBy` and for a player
who buys for themselves. A token carries only the Zitadel `sub`, and only
service-player maps it ([0014](0014-player-publicid-is-the-cross-service-identifier.md)).
A copy of that mapping in each service, or a call to service-player per
request, makes every service depend on service-player.

## Decision

We will store `publicId` as Zitadel user metadata. service-player writes it at
provisioning, before it grants the `player` role
([0018](0018-zitadel-provisions-players-and-me-is-the-fallback.md)). Clients
request the scope `urn:zitadel:iam:user:metadata`, and services read the claim
from the access token.

## Consequences

- No service asks service-player who is calling, and no login waits for it.
- A player cannot write their own metadata. Anyone with user-write rights in
  Zitadel can, so those rights now allow impersonation.
- Without the role, Zitadel refuses the login. A role granted by hand skips
  that gate, so `ProvisioningReconciler` also writes missing metadata.
- Values arrive base64-encoded, and every metadata key reaches every token.
- A service rejects a write whose token has no claim, so every client asks for the scope.
