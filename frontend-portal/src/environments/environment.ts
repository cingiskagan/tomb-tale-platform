/**
 * Development environment configuration.
 *
 * Connects to local Zitadel instance and backend services
 * running via docker-compose in the infrastructure directory.
 */
export const environment = {
  production: false,
  zitadelIssuerUri: 'http://localhost:8080',
  zitadelClientId: '365846455157522438',
  apiBaseUrl: 'http://localhost:8082',
} as const;
