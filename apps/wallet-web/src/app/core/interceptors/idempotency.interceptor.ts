import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { LoggerService } from '@core/infrastructure/logger.service';

/**
 * Idempotency interceptor — preserves an existing `Idempotency-Key` header.
 *
 * The key is generated upstream (store) and travels with the request
 * through the adapter. This interceptor ensures it is NOT stripped by
 * Angular's HttpHandler chain. It does **not** generate or overwrite keys.
 *
 * Requests without a key pass through untouched.
 */
export const idempotencyInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.headers.has('Idempotency-Key')) {
    return next(req);
  }

  const logger = inject(LoggerService);
  logger.debug('Preserved Idempotency-Key', 'IdempotencyInterceptor', {
    method: req.method,
    url: req.url,
  });

  return next(req.clone({
    setHeaders: { 'Idempotency-Key': req.headers.get('Idempotency-Key')! },
  }));
};
