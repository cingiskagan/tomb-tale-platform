# 25. nginx replaces Traefik, and `/api` still belongs to the platform

- **Date:** 2026-10-06
- **Status:** accepted

## Context

Traefik arrived with Zitadel, which needed h2c for gRPC and label-driven
routes for its login container. Keycloak needs neither. Traefik also mounted
the Docker socket, which gives the proxy root-level access to the host, and a
deployed portal needs a web server for its files, which Traefik cannot be.

## Decision

We will route with nginx, from one file, `infrastructure/nginx.conf`. Each
service prefix has a `location`, Keycloak gets `/realms/` and `/resources/`,
and every other path answers 404. The rule of ADR 0021 stands: `/api` belongs
to the platform, and an unknown prefix never reaches the identity provider.

## Consequences

- A new controller prefix needs its `location` in `nginx.conf`.
- No container mounts the Docker socket.
- Supersedes 0021, whose mechanism was Traefik routers.
- TLS is not set up. Traefik would renew certificates itself, while nginx
  needs certbot beside it, which the deployment work (G1) decides.

---

*Implemented by:* `feat(infra): nginx replaces Traefik`
