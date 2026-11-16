import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { RUNTIME_CONFIG } from '../config';
import { LoadingService } from './loading.service';

/**
 * Counts each call to the platform API until it answers, fails or is cancelled. Keycloak's calls don't count, so a
 * token refresh in the background shows no loading.
 */
export const loadingInterceptor: HttpInterceptorFn = (request, next) => {
  const loading = inject(LoadingService);
  if (!request.url.startsWith(inject(RUNTIME_CONFIG).apiBaseUrl)) return next(request);
  loading.start();
  return next(request).pipe(finalize(() => loading.stop()));
};
