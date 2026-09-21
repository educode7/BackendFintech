import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Request ID interceptor.
 * Generates X-Request-Id and attaches client metadata headers.
 * Skips Keycloak/OIDC endpoints (CORS doesn't allow custom headers).
 */
export const requestIdInterceptor: HttpInterceptorFn = (req, next) => {
  // Skip Keycloak/OIDC endpoints — CORS doesn't allow custom headers
  if (req.url.includes('/realms/') || req.url.includes('/protocol/')) {
    return next(req);
  }

  const requestId = req.headers.get('X-Request-Id') ?? crypto.randomUUID();

  const cloned = req.clone({
    setHeaders: {
      'X-Request-Id': requestId,
      'X-Client-Version': '0.0.0',
      'X-Client-Platform': 'web',
    },
  });

  return next(cloned);
};
