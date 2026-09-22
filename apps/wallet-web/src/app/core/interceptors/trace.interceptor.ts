import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { SpanKind, SpanStatusCode, context, propagation, trace } from '@opentelemetry/api';
import { finalize, tap } from 'rxjs';
import { LoggerService } from '@core/infrastructure/logger.service';

/**
 * Trace interceptor — creates a real CLIENT span per API call and
 * propagates W3C Trace Context from that span via OTel API.
 *
 * - Keycloak/OIDC endpoints are skipped (their CORS rejects custom headers).
 * - An inbound traceparent is never overwritten (upstream propagation wins).
 * - On failure the error is recorded on the span with SpanStatusCode.ERROR;
 *   the span is always ended when the request observable settles.
 */
export const traceInterceptor: HttpInterceptorFn = (req, next) => {
  // Skip Keycloak/OIDC endpoints — CORS doesn't allow custom headers
  if (req.url.includes('/realms/') || req.url.includes('/protocol/')) {
    return next(req);
  }

  // Do not overwrite existing traceparent — preserve upstream propagation
  if (req.headers.has('traceparent')) {
    return next(req);
  }

  const logger = inject(LoggerService);
  const tracer = trace.getTracer('wallet-web');
  const span = tracer.startSpan(`${req.method} ${pathOf(req.url)}`, {
    kind: SpanKind.CLIENT,
    attributes: {
      'http.request.method': req.method,
      'url.full': req.url,
    },
  });

  // Inject W3C traceparent (and baggage) from the active span context —
  // the sampled flag and IDs come from the real span, not from random bytes.
  const carrier: Record<string, string> = {};
  propagation.inject(trace.setSpan(context.active(), span), carrier);

  const cloned = req.clone({ setHeaders: carrier });

  logger.debug('Started client span', 'TraceInterceptor', {
    traceId: span.spanContext().traceId,
    spanId: span.spanContext().spanId,
    method: req.method,
    url: req.url,
  });

  return next(cloned).pipe(
    tap({
      error: (err) => {
        span.recordException(err);
        span.setStatus({
          code: SpanStatusCode.ERROR,
          message: err?.message ?? String(err),
        });
      },
    }),
    // Equivalent of finally: runs on complete, error, and unsubscribe.
    finalize(() => span.end()),
  );
};

/**
 * Extract the URL path so span names stay low-cardinality (`GET /api/x`).
 */
function pathOf(url: string): string {
  try {
    return new URL(url, 'http://localhost').pathname;
  } catch {
    return url;
  }
}
