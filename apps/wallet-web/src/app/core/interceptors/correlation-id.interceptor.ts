import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Correlation ID interceptor.
 * Attaches X-Correlation-Id (UUID v7) to every outgoing request.
 * Skips Keycloak/OIDC endpoints (CORS doesn't allow custom headers).
 */
export const correlationIdInterceptor: HttpInterceptorFn = (req, next) => {
  // Skip Keycloak/OIDC endpoints — CORS doesn't allow custom headers
  if (req.url.includes('/realms/') || req.url.includes('/protocol/')) {
    return next(req);
  }

  const correlationId = crypto.randomUUID();
  const cloned = req.clone({
    setHeaders: { 'X-Correlation-Id': correlationId },
  });
  return next(cloned);
};
