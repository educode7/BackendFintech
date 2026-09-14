import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '@env/environment';

/**
 * Correlation ID interceptor.
 * Attaches X-Correlation-Id (UUID v7) to every outgoing request.
 */
export const correlationIdInterceptor: HttpInterceptorFn = (req, next) => {
  const correlationId = crypto.randomUUID();
  const cloned = req.clone({
    setHeaders: { 'X-Correlation-Id': correlationId },
  });
  return next(cloned);
};
