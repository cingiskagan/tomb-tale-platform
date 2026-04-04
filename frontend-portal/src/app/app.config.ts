import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideOAuthClient } from 'angular-oauth2-oidc';
import { providePrimeNG } from 'primeng/config';
import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth';

/**
 * Custom PrimeNG theme preset for Tomb Tale.
 *
 * Extends the Aura base preset with dark RPG-inspired colors:
 * - Gold/amber primary palette (ancient treasure)
 * - Deep obsidian surface colors
 * - Emerald secondary accents
 */
const TombTalePreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '{amber.50}',
      100: '{amber.100}',
      200: '{amber.200}',
      300: '{amber.300}',
      400: '{amber.400}',
      500: '{amber.500}',
      600: '{amber.600}',
      700: '{amber.700}',
      800: '{amber.800}',
      900: '{amber.900}',
      950: '{amber.950}',
    },
    colorScheme: {
      dark: {
        surface: {
          0: '#ffffff',
          50: '#e8e0d4',
          100: '#c4b9a8',
          200: '#8a8078',
          300: '#5a524a',
          400: '#3a3430',
          500: '#252220',
          600: '#1a1818',
          700: '#141214',
          800: '#0f0f19',
          900: '#0a0a0f',
          950: '#050508',
        },
      },
    },
  },
});

/**
 * Root application configuration.
 *
 * Registers all providers required for:
 * - Routing with lazy-loaded guards
 * - HTTP client with Bearer token interceptor
 * - Browser animations for PrimeNG transitions
 * - OAuth2/OIDC client for Zitadel authentication
 * - PrimeNG component library with Tomb Tale dark theme
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimationsAsync(),
    provideOAuthClient(),
    providePrimeNG({
      theme: {
        preset: TombTalePreset,
        options: {
          darkModeSelector: '.tomb-tale-dark',
        },
      },
    }),
  ],
};
