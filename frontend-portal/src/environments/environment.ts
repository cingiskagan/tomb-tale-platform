/**
 * Development build flags.
 *
 * Only what the build itself decides lives here. The Zitadel issuer, client id
 * and API base URL are read at startup from `config.json` — see
 * `core/config/runtime-config.ts`.
 */
export const environment = {
  production: false,
} as const;
