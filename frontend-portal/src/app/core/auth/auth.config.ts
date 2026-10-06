import { AuthConfig } from 'angular-oauth2-oidc';
import { environment } from '../../../environments/environment';
import { RuntimeConfig } from '../config';

/**
 * OIDC configuration for Keycloak authentication.
 *
 * Uses Authorization Code flow with PKCE (Proof Key for Code Exchange),
 * which is the recommended flow for public clients like SPAs.
 * The login UI is fully delegated to Keycloak's login pages.
 */

// The roles and public_id claims come from the client's mappers, not from a
// scope (ADR 0024). Without offline_access, the refresh token ends with the session.
const OIDC_SCOPES = ['openid', 'profile', 'email'].join(' ');

/** Builds the OIDC config from the settings fetched at startup. */
export function buildAuthConfig(config: RuntimeConfig): AuthConfig {
  return {
    issuer: config.issuerUri,
    redirectUri: `${globalThis.location.origin}/callback`,
    postLogoutRedirectUri: `${globalThis.location.origin}/`,
    clientId: config.clientId,
    responseType: 'code',
    scope: OIDC_SCOPES,
    showDebugInformation: !environment.production,
    requireHttps: environment.production,
  };
}
