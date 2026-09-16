import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { switchMap, catchError, throwError } from 'rxjs';
import { AuthService } from '@core/infrastructure/auth.service';

/**
 * Transparent token refresh interceptor.
 *
 * - Detects 401 responses and attempts a single refresh-retry cycle.
 * - Queues concurrent requests while a refresh is in flight.
 * - Skips the refresh and revoke endpoints to avoid infinite loops.
 * - Pre-emptively refreshes when the access token expires within 60 s.
 */
export const tokenRefreshInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  // Never intercept the refresh/revoke endpoints themselves
  if (req.url.includes('/auth/refresh') || req.url.includes('/auth/revoke')) {
    return next(req);
  }

  // Pre-emptive refresh when token is about to expire
  if (auth.isTokenExpiringSoon(60)) {
    return auth.refreshAccessToken().pipe(
      switchMap(() => next(req))
    );
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401) {
        return throwError(() => error);
      }

      // Attempt a single refresh-retry cycle
      return auth.refreshAccessToken().pipe(
        switchMap(() => next(req)),
        catchError((refreshError) => {
          auth.clearToken();
          return throwError(() => refreshError);
        })
      );
    })
  );
};
