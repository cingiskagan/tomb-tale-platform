# CLAUDE.md

## Tests

```bash
npx ng test --watch=false --browsers=ChromeHeadless     # headless run (CHROME_BIN must point to a Chrome/Chromium binary)
```

## Routes

`app.routes.ts` nests all authenticated routes under `MainLayoutComponent` behind `authGuard`, with `playerProfileResolver` resolving once for the whole subtree; feature components are lazy-loaded via `loadComponent`. Role-gated routes (`/purchases`, `/players`) add `roleGuard` + `data: { roles: [...] }` — use this pattern for new role-restricted routes rather than checking roles inside components.

## Runtime config

`frontend-portal/public/config.json` holds the Keycloak issuer, client id and API base URL. It is tracked with the dev values, a deployment replaces it, and `main.ts` fetches it before the app bootstraps, so nothing reads these values at import time. `src/environments/environment*.ts` keep only the `production` flag. The values must stay consistent with `infrastructure/.env` and the realm file. See ADR 0020 and ADR 0024.
