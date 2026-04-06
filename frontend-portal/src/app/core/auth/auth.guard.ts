import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Functional route guard that protects routes requiring authentication.
 *
 * Checks if the user has a valid access token via AuthService.
 * If not authenticated, returns a UrlTree to redirect to the login page.
 * Uses Angular's functional guard API (CanActivateFn) for a lighter,
 * tree-shakeable approach vs. class-based guards.
 */
export const authGuard: CanActivateFn = async (): Promise<boolean | UrlTree> => {
  const authService = inject(AuthService);
  const router = inject(Router);

  try {
    await authService.initializeAuth();
    return authService.isAuthenticated() ? true : router.parseUrl('/login');
  } catch {
    return router.parseUrl('/login');
  }
};
