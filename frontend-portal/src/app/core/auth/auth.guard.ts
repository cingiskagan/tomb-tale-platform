import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Functional route guard that protects routes requiring authentication.
 *
 * Checks if the user has a valid access token via AuthService.
 * If not authenticated, redirects to the login page.
 * Uses Angular's functional guard API (CanActivateFn) for a lighter,
 * tree-shakeable approach vs. class-based guards.
 */
export const authGuard: CanActivateFn = (): boolean => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};
