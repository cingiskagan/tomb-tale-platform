/**
 * Production environment configuration.
 *
 * These values should be overridden during the CI/CD build pipeline
 * using Angular's fileReplacements in angular.json.
 */
export const environment = {
  production: true,
  zitadelIssuerUri: 'https://auth.tombtale.com',
  zitadelClientId: 'REPLACE_IN_CI',
  apiBaseUrl: 'https://api.tombtale.com',
} as const;
