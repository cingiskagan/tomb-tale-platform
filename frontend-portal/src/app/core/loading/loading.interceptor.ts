import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { RUNTIME_CONFIG } from '../config';
import { LoadingService } from './loading.service';

/**
 * Counts each call to the platform API until it answers, fails or is cancelled, and marks every call but a GET as a
 * change. Keycloak's calls don't count, so a token refresh in the background shows no loading.
 */
export const loadingInterceptor: HttpInterceptorFn = (request, next) => {
  const loading = inject(LoadingService);
  if (!request.url.startsWith(inject(RUNTIME_CONFIG).apiBaseUrl)) return next(request);
  loading.start(request.method !== 'GET' && request.method !== 'HEAD');
  return next(request).pipe(finalize(() => loading.stop()));
};
