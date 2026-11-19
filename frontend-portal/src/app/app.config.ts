import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideOAuthClient } from 'angular-oauth2-oidc';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth';
import { loadingInterceptor } from './core/loading';

/**
 * Root application configuration.
 *
 * Registers all providers required for:
 * - Routing with lazy-loaded guards
 * - HTTP client with Bearer token and loading interceptors
 * - OAuth2/OIDC client for Keycloak authentication
 * - PrimeIcons as mat-icon's font, so `<mat-icon fontIcon="pi-check" />` draws one
 *
 * Angular Material needs no provider. Its theme lives in styles.scss.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor, loadingInterceptor])),
    provideOAuthClient(),
    provideAppInitializer(() => {
      inject(MatIconRegistry).setDefaultFontSetClass('pi');
    }),
  ],
};
