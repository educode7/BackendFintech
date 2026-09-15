import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { LoggerService } from '@core/services/logger.service';

/**
 * Trace interceptor.
 * Injects W3C traceparent header for distributed tracing.
 *
 * Format: 00-{trace-id}-{span-id}-01
 * In production, this would use OpenTelemetry propagation.inject().
 * For now, generates a trace context manually.
 */
export const traceInterceptor: HttpInterceptorFn = (req, next) => {
  const logger = inject(LoggerService);

  const traceId = generateTraceId();
  const spanId = generateSpanId();
  const traceparent = `00-${traceId}-${spanId}-01`;

  // Do not overwrite existing traceparent — preserve upstream propagation
  const headers: Record<string, string> = req.headers.has('traceparent')
    ? {}
    : { traceparent };

  const cloned = req.clone({ setHeaders: headers });

  logger.debug('Injected traceparent', 'TraceInterceptor', {
    traceId,
    spanId,
    method: req.method,
    url: req.url,
  });

  return next(cloned);
};

/**
 * Generate 32-character hex trace ID per W3C spec.
 */
function generateTraceId(): string {
  return Array.from(crypto.getRandomValues(new Uint8Array(16)))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

/**
 * Generate 16-character hex span ID per W3C spec.
 */
function generateSpanId(): string {
  return Array.from(crypto.getRandomValues(new Uint8Array(8)))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}
