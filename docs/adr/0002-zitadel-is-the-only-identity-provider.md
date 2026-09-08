# 2. Zitadel is the only identity provider

- **Date:** 2026-03-12
- **Status:** accepted

## Context

The portal needs users to log in, and both backend services need to know who is
calling and what they are allowed to do. More services are planned, and they
will need the same thing.

Building this ourselves means password hashing, reset flows, session storage, an
account admin screen and an audit trail. None of that is game code, and all of
it is the kind of code that is quietly wrong until it is expensively wrong.

The services also have to stay stateless. Any instance must be able to serve any
request, which rules out a server-side session as the thing that carries
identity.

## Decision

We will run Zitadel as the only identity provider, self-hosted in the Compose
stack and reachable through Traefik on port 8080.

The portal runs the OIDC authorization code flow against Zitadel and receives a
JWT. It sends that token to the services directly, in an `Authorization: Bearer`
header. Both services are stateless OAuth2 resource servers: they know only
`issuer-uri`, they fetch signing keys from it, and they create no session.

Authorization comes from Zitadel project roles carried in the token. There are
three: `player`, `game_master`, `platform_admin`.

## Consequences

- Neither service has a user table, a login endpoint or a password to store.
  They contact Zitadel only to validate tokens.
- Zitadel puts project roles in a claim named
  `urn:zitadel:iam:org:project:roles`, and its value is a map keyed by role
  name, not a list of strings. Spring's default converter reads `scope` and
  finds nothing there, so `ZitadelRoleConverter` exists to read that claim and
  merge the role names into the granted authorities.
- Zitadel issues bare role names with no `ROLE_` prefix. Endpoints must guard
  with `hasAuthority('platform_admin')` and never `hasRole(...)`, which would
  look for a prefix that is never there. That mistake denies everyone rather
  than admitting everyone, so it fails safe — but it fails silently at runtime,
  not at compile time.
- The role names are a contract in three places: the Zitadel project
  configuration, the `@PreAuthorize` strings in both services, and the
  `PlatformRole` enum in the portal. All three change together or authorization
  breaks.
- Because sessions are stateless and the token travels in a header rather than a
  cookie, CSRF protection is disabled. That is correct only for as long as no
  cookie-based authentication is ever added next to it.
- Local development depends on the whole Compose stack. Without Zitadel running
  there is no way to obtain a real token, so a service cannot be exercised
  end to end on its own.
