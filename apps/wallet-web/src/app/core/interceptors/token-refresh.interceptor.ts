import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { OAuthService } from 'angular-oauth2-oidc';

/**
 * Token interceptor — attaches the access token to all API requests.
 *
 * Uses angular-oauth2-oidc for token management:
 * - Token attachment: OAuthService.getAccessToken()
 * - Token refresh: handled by OAuthService via session checks / silent refresh
 * - 401 handling: redirect to Keycloak login
 */
export const tokenRefreshInterceptor: HttpInterceptorFn = (req, next) => {
  const oauthService = inject(OAuthService);

  // Skip auth for Keycloak/OIDC endpoints
  if (req.url.includes('/realms/') || req.url.includes('/protocol/')) {
    return next(req);
  }

  // Attach access token to all API requests
  let authReq = req;
  if (oauthService.hasValidAccessToken()) {
    const token = oauthService.getAccessToken();
    authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Token expired or invalid — redirect to Keycloak login
        oauthService.initLoginFlow();
      }
      return throwError(() => error);
    })
  );
};
