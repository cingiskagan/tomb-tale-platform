# 20. The portal reads its configuration at runtime

- **Date:** 2026-09-19
- **Status:** accepted

## Context

Zitadel generates the OIDC client id, so `zitadel-setup.sh` wrote it into the
tracked `environment.ts`, and every rebuild produced a diff that meant nothing.
[ADR 0019](0019-zitadel-configuration-is-code.md) put it there.

## Decision

We will serve the client id, issuer and API base URL from a gitignored
`frontend-portal/public/config.json`. `main.ts` fetches it before
`bootstrapApplication` and provides `RUNTIME_CONFIG`, and `environment.ts` keeps
only `production`.

## Consequences

- A rebuild touches no tracked file, and the portal repoints without one.
- Startup now depends on a fetch that can fail. The loader validates all three
  keys and names the script, and `npm start` needs that script to have run.
- Import-time reads are gone: `AUTH_CONFIG` is now `buildAuthConfig(config)`.
- `.env` gained `PORTAL_API_BASE_URL`, which the script requires.

---

*Implemented by:* `feat(portal): read Zitadel config at runtime`
