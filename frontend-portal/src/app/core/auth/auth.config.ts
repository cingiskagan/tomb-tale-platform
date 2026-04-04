import { AuthConfig } from 'angular-oauth2-oidc';
import { environment } from '../../../environments/environment';

/**
 * OIDC configuration for Zitadel authentication.
 *
 * Uses Authorization Code flow with PKCE (Proof Key for Code Exchange),
 * which is the recommended flow for public clients like SPAs.
 * The login UI is fully delegated to Zitadel's hosted login page.
 */

const OIDC_SCOPES = 'openid profile email offline_access';

export const AUTH_CONFIG: AuthConfig = {
  issuer: environment.zitadelIssuerUri,
  redirectUri: `${globalThis.location.origin}/callback`,
  postLogoutRedirectUri: globalThis.location.origin,
  clientId: environment.zitadelClientId,
  responseType: 'code',
  scope: OIDC_SCOPES,
  showDebugInformation: !environment.production,
  requireHttps: environment.production,
};
