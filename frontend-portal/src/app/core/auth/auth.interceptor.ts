import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { RUNTIME_CONFIG } from '../config';

/**
 * Functional HTTP interceptor that attaches the Bearer access token
 * to outgoing requests targeting the platform API.
 *
 * Only modifies requests to the configured API base URL to avoid
 * leaking tokens to third-party endpoints.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const { apiBaseUrl } = inject(RUNTIME_CONFIG);

  const isApiRequest = request.url.startsWith(apiBaseUrl);
  if (!isApiRequest || !authService.isAuthenticated()) {
    return next(request);
  }

  const authenticatedRequest = request.clone({
    setHeaders: {
      Authorization: `Bearer ${authService.getAccessToken()}`,
    },
  });

  return next(authenticatedRequest);
};
