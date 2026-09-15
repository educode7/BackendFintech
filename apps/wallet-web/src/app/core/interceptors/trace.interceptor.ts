import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { trace, context, propagation } from '@opentelemetry/api';
import { LoggerService } from '@core/infrastructure/logger.service';

/**
 * Trace interceptor — propagates W3C Trace Context via OTel API.
 *
 * If an active span exists (from auto-instrumentation), injects its context.
 * Otherwise generates a new traceparent header manually.
 */
export const traceInterceptor: HttpInterceptorFn = (req, next) => {
  const logger = inject(LoggerService);
  const tracer = trace.getTracer('wallet-web');

  // Check if there's an active span from auto-instrumentation
  const activeSpan = trace.getActiveSpan();

  let traceId: string;
  let spanId: string;

  if (activeSpan) {
    // Use the active span's context
    const spanContext = activeSpan.spanContext();
    traceId = spanContext.traceId;
    spanId = spanContext.spanId;
  } else {
    // Generate new IDs (fallback when no active span)
    traceId = generateTraceId();
    spanId = generateSpanId();
  }

  // Do not overwrite existing traceparent — preserve upstream propagation
  if (req.headers.has('traceparent')) {
    return next(req);
  }

  const traceparent = `00-${traceId}-${spanId}-01`;

  // Also inject baggage via OTel propagation
  const carrier: Record<string, string> = {};
  propagation.inject(context.active(), carrier);

  const cloned = req.clone({
    setHeaders: {
      traceparent,
      ...carrier,
    },
  });

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
