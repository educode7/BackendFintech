import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { LoggerService } from '@core/services/logger.service';

/**
 * Idempotency interceptor.
 * Generates and attaches Idempotency-Key for state-changing requests (POST, PATCH, PUT).
 */
export const idempotencyInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.method === 'GET' || req.method === 'DELETE' || req.method === 'HEAD' || req.method === 'OPTIONS') {
    return next(req);
  }

  const idempotencyKey = crypto.randomUUID();
  const cloned = req.clone({
    setHeaders: { 'Idempotency-Key': idempotencyKey },
  });

  const logger = inject(LoggerService);
  logger.debug('Attached Idempotency-Key', 'IdempotencyInterceptor', {
    method: req.method,
    url: req.url,
  });

  return next(cloned);
};
