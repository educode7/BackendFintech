import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { switchMap, catchError, throwError } from 'rxjs';
import { AuthService } from '@core/infrastructure/auth.service';

/**
 * Transparent token refresh interceptor.
 *
 * Token strategy:
 * - Access token: attached from memory via AuthService.getToken()
 * - Refresh token: HttpOnly Secure cookie (sent automatically by the browser)
 *
 * Flow on 401:
 * 1. The access token is expired or invalid
 * 2. Interceptor calls refreshAccessToken() → POST /auth/refresh with credentials
 * 3. Backend reads refresh_token from HttpOnly cookie
 * 4. Backend returns new access_token + sets new refresh_token cookie
 * 5. Original request is retried with the new access token
 *
 * Skips refresh/revoke endpoints to avoid infinite loops.
 */
export const tokenRefreshInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  // Never intercept the refresh/revoke endpoints themselves
  if (req.url.includes('/auth/refresh') || req.url.includes('/auth/revoke')) {
    // But still attach access token for /auth/revoke (it needs identity)
    if (req.url.includes('/auth/revoke') && auth.getToken()) {
      const cloned = req.clone({
        setHeaders: { Authorization: `Bearer ${auth.getToken()}` },
      });
      return next(cloned);
    }
    return next(req);
  }

  // Attach access token to all other requests
  let authReq = req;
  if (auth.getToken()) {
    authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${auth.getToken()}` },
    });
  }

  // Pre-emptive refresh when token is about to expire
  if (auth.isTokenExpiringSoon(60)) {
    return auth.refreshAccessToken().pipe(
      switchMap(() => {
        const retryReq = req.clone({
          setHeaders: { Authorization: `Bearer ${auth.getToken()}` },
        });
        return next(retryReq);
      })
    );
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401) {
        return throwError(() => error);
      }

      // Attempt a single refresh-retry cycle
      // The refresh_token cookie is sent automatically — no need to include it in the body
      return auth.refreshAccessToken().pipe(
        switchMap(() => {
          const retryReq = req.clone({
            setHeaders: { Authorization: `Bearer ${auth.getToken()}` },
          });
          return next(retryReq);
        }),
        catchError((refreshError) => {
          auth.clearToken();
          return throwError(() => refreshError);
        })
      );
    })
  );
};
