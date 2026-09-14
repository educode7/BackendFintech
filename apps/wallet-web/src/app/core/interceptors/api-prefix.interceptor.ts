import { HttpInterceptorFn } from '@angular/common/http';

/**
 * API prefix interceptor.
 * Prepends the gateway base URL to relative paths.
 */
export const apiPrefixInterceptor: HttpInterceptorFn = (req, next) => {
  // Only prefix relative URLs (skip absolute URLs like http://...)
  if (req.url.startsWith('http')) {
    return next(req);
  }
  const apiReq = req.clone({ url: `/api${req.url}` });
  return next(apiReq);
};
