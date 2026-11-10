import { HttpErrorResponse } from '@angular/common/http';

/** The `detail` of the backend's problem response (ADR 0015), or a fallback when it has none. */
export function problemDetail(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse && typeof error.error?.detail === 'string') {
    return error.error.detail;
  }
  return fallback;
}
