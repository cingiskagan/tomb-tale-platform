# 2. Zitadel is the only identity provider

- **Date:** 2026-03-12
- **Status:** accepted

## Context

The portal needs a login, and both services need to know who calls them and what
that caller is allowed to do. Building that ourselves means passwords, reset
flows, sessions and an audit trail, and the services must stay stateless.

## Decision

We will run Zitadel as the only identity provider, self-hosted in the Compose
stack behind Traefik on port 8080. The portal runs the OIDC code flow and sends
the JWT to both services, stateless resource servers that hold only
`issuer-uri`. Three roles authorize: `player`, `game_master`, `platform_admin`.

## Consequences

- Neither service holds a user table, a login endpoint or a password. Neither
  runs end to end without the stack, because only Zitadel issues a real token.
- Zitadel puts roles in `urn:zitadel:iam:org:project:roles` as a map, not in
  `scope`, so `ZitadelRoleConverter` reads that claim.
- Role names carry no `ROLE_` prefix. Endpoints guard with `hasAuthority`,
  because `hasRole` compiles and then denies everyone at runtime.
- The three names are a contract in Zitadel, in `@PreAuthorize` and in the
  portal `PlatformRole` enum. All three change together.
- The token travels in a header, so CSRF protection is off. It stays off only
  while no cookie authentication is added beside it.
