import { Injectable, inject } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { BehaviorSubject } from 'rxjs';
import { AUTH_CONFIG } from './auth.config';

/** Decoded identity claims from the Zitadel ID token. */
export interface UserProfile {
  readonly sub: string;
  readonly name?: string;
  readonly email?: string;
  readonly preferred_username?: string;
}

/**
 * Wraps the OAuthService to provide a clean, domain-specific API
 * for authentication operations throughout the application.
 *
 * Responsibilities:
 * - Initialize OIDC discovery and attempt silent login on app startup
 * - Trigger the PKCE authorization code flow
 * - Expose reactive auth state and user profile
 * - Provide the access token for API interceptors
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authenticatedSubject = new BehaviorSubject<boolean>(false);

  /** Emits `true` when the user holds a valid access token. */
  private readonly oauthService = inject(OAuthService);
  private initPromise?: Promise<void>;

  constructor() {
    this.oauthService.configure(AUTH_CONFIG);
    this.oauthService.setupAutomaticSilentRefresh();
  }

  /**
   * Bootstraps the OIDC flow: loads the discovery document from
   * Zitadel's well-known endpoint and attempts to process any
   * existing authorization code (e.g. after redirect from login).
   * * If initialization fails (e.g., network error), the promise cache is 
   * cleared to allow subsequent retries.
   */
  initializeAuth(): Promise<void> {
    this.initPromise ??= this._initializeAuth().catch((error) => {
      this.initPromise = undefined;
      throw error;
    });
    return this.initPromise;
  }

  private async _initializeAuth(): Promise<void> {
    await this.oauthService.loadDiscoveryDocumentAndTryLogin();
    this.authenticatedSubject.next(this.oauthService.hasValidAccessToken());

    this.oauthService.events.subscribe(() => {
      this.authenticatedSubject.next(
        this.oauthService.hasValidAccessToken()
      );
    });
  }

  /** Redirects the user to Zitadel's hosted login page via PKCE flow. */
  login(): void {
    this.oauthService.initCodeFlow();
  }

  /** Performs an OIDC RP-initiated logout and redirects back to the app. */
  logout(): void {
    this.oauthService.logOut();
  }

  /** Returns true if the current access token is valid and not expired. */
  isAuthenticated(): boolean {
    return this.oauthService.hasValidAccessToken();
  }

  /**
   * Extracts user profile claims from the decoded ID token.
   * Returns `null` if no valid ID token is present.
   */
  getUserProfile(): UserProfile | null {
    const claims = this.oauthService.getIdentityClaims() as Record<string, unknown> | null;
    if (!claims) {
      return null;
    }

    if (typeof claims['sub'] !== 'string' || claims['sub'].trim() === '') {
      return null;
    }

    return {
      sub: claims['sub'],
      name: typeof claims['name'] === 'string' ? claims['name'] : undefined,
      email: typeof claims['email'] === 'string' ? claims['email'] : undefined,
      preferred_username: typeof claims['preferred_username'] === 'string' ? claims['preferred_username'] : undefined,
    };
  }

  /** Returns the current Bearer access token for API calls. */
  getAccessToken(): string {
    return this.oauthService.getAccessToken();
  }
}
