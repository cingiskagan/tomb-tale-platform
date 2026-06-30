import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { PlatformRole } from './auth.models';

/**
 * Route guard that checks if the authenticated user has any of the required roles.
 * The required roles should be specified in the route's data object under the 'roles' key.
 * 
 * Example:
 * canActivate: [roleGuard],
 * data: { roles: [PlatformRole.PLATFORM_ADMIN, PlatformRole.GAME_MASTER] }
 */
export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // If the user is not authenticated at all, let the authGuard handle it,
  // or redirect to login. Usually authGuard runs first, but just in case:
  if (!authService.isAuthenticated()) {
    return router.parseUrl('/login');
  }

  const requiredRoles = route.data['roles'] as PlatformRole[];
  const userProfile = authService.getUserProfile();
  const userRoles = userProfile || [];

  console.log(`[RoleGuard] Navigating to: ${state.url}`);
  console.log(`[RoleGuard] Required Roles:`, requiredRoles);
  console.log(`[RoleGuard] User Object:`, userRoles);

  // If no roles are required for this route, allow access
  if (!requiredRoles || requiredRoles.length === 0) {
    console.log(`[RoleGuard] Access GRANTED (No roles required)`);
    return true;
  }

  const hasRole = authService.hasAnyRole(requiredRoles);
  console.log(`[RoleGuard] Has Required Role? ${hasRole}`);

  if (!hasRole) {
    // User is authenticated but lacks required roles, redirect to default dashboard
    console.warn(`[RoleGuard] Access DENIED. Redirecting to /dashboard`);
    return router.parseUrl('/dashboard');
  }

  return true;
};
