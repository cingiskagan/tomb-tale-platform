import { AuthConfig } from 'angular-oauth2-oidc';
import { environment } from '../../../environments/environment';
import { RuntimeConfig } from '../config';

/**
 * OIDC configuration for Zitadel authentication.
 *
 * Uses Authorization Code flow with PKCE (Proof Key for Code Exchange),
 * which is the recommended flow for public clients like SPAs.
 * The login UI is fully delegated to Zitadel's hosted login page.
 */

const OIDC_SCOPES = [
  'openid',
  'profile',
  'email',
  'offline_access',
  'urn:zitadel:iam:org:project:roles',
  // Puts the player's publicId in the access token (ADR 0023).
  'urn:zitadel:iam:user:metadata',
].join(' ');

/** Builds the OIDC config from the settings fetched at startup. */
export function buildAuthConfig(config: RuntimeConfig): AuthConfig {
  return {
    issuer: config.zitadelIssuerUri,
    redirectUri: `${globalThis.location.origin}/callback`,
    postLogoutRedirectUri: `${globalThis.location.origin}/`,
    clientId: config.zitadelClientId,
    responseType: 'code',
    scope: OIDC_SCOPES,
    showDebugInformation: !environment.production,
    requireHttps: environment.production,
  };
}
