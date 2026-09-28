# 21. `/api` belongs to the platform, not to Zitadel

- **Date:** 2026-09-28
- **Status:** accepted

## Context

Traefik serves Zitadel and the platform services on one origin,
`localhost:8080`. A Zitadel router took every `/api` path, stripped the prefix
and forwarded it to Zitadel. A controller prefix with no router of its own went
to the identity provider, not to a 404. `/api/v1/characters` is one example.
Nothing in the repository called Zitadel through that alias.

## Decision

We will reserve `/api` for the platform services. Zitadel gets no `/api` router,
and `zitadel-canonical-web` excludes `/api`. Each service prefix has a router in
`infrastructure/traefik-dynamic.yml`, and an `/api` path with no router gets
Traefik's own 404.

## Consequences

- A forgotten prefix fails with a 404 and never reaches Zitadel.
- A new controller prefix still needs its own router in `traefik-dynamic.yml`.
- The `/api` exclusion in `zitadel-canonical-web` is now the only guard. If it
  goes, Zitadel gets every unrouted `/api` path again.

---

*Implemented by:* `fix(infra): safe Traefik routing`
