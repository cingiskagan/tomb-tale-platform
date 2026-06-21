/**
 * Development environment configuration.
 *
 * Connects to local Zitadel instance and backend services
 * running via docker-compose in the infrastructure directory.
 */
export const environment = {
  production: false,
  zitadelIssuerUri: 'http://localhost:8080',
  zitadelClientId: '378280910304378886',
  apiBaseUrl: 'http://localhost:8080',
} as const;
