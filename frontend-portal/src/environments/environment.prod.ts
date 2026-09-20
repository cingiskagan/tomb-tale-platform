/**
 * Production build flags.
 *
 * Not reachable yet: angular.json has no `fileReplacements`, so every build
 * uses environment.ts. Everything that varies per environment rather than per
 * build is in `config.json` instead.
 */
export const environment = {
  production: true,
} as const;
