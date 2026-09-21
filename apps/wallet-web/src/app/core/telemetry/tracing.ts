import {
  CompositePropagator,
  W3CBaggagePropagator,
  W3CTraceContextPropagator,
} from '@opentelemetry/core';
import { WebTracerProvider } from '@opentelemetry/sdk-trace-web';
import { SimpleSpanProcessor, ConsoleSpanExporter } from '@opentelemetry/sdk-trace-base';
import { resourceFromAttributes } from '@opentelemetry/resources';
import { ATTR_SERVICE_NAME, ATTR_SERVICE_VERSION } from '@opentelemetry/semantic-conventions';
import { environment } from '@env/environment';

/**
 * Initialize OpenTelemetry tracing for the browser.
 *
 * - Development: ConsoleSpanExporter only (visible in DevTools)
 * - Production: OTLP exporter + ConsoleSpanExporter
 *
 * Auto-instrumentation is NOT used because it patches fetch/XHR at the
 * browser level and adds traceparent to ALL requests, including Keycloak
 * OIDC requests that have strict CORS policies. The traceInterceptor
 * handles trace propagation for backend API requests instead.
 *
 * Call once in app bootstrap (main.ts).
 */
export async function initializeTracing(): Promise<void> {
  const resource = resourceFromAttributes({
    [ATTR_SERVICE_NAME]: 'wallet-web',
    [ATTR_SERVICE_VERSION]: '0.1.0',
  });

  const consoleExporter = new ConsoleSpanExporter();
  const spanProcessors = [new SimpleSpanProcessor(consoleExporter)];

  // OTLP exporter — only in production (collector must be running)
  if (environment.production) {
    const endpoint =
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (globalThis as any).env?.['OTEL_EXPORTER_OTLP_ENDPOINT'] ?? 'http://localhost:4318';
    const { OTLPTraceExporter } = await import('@opentelemetry/exporter-trace-otlp-http');
    const otlpExporter = new OTLPTraceExporter({
      url: `${endpoint}/v1/traces`,
    });
    spanProcessors.push(new SimpleSpanProcessor(otlpExporter));
  }

  const provider = new WebTracerProvider({
    resource,
    spanProcessors,
  });

  provider.register({
    propagator: new CompositePropagator({
      propagators: [
        new W3CBaggagePropagator(),
        new W3CTraceContextPropagator(),
      ],
    }),
  });

  // NOTE: Auto-instrumentation removed — it patches fetch/XHR globally
  // and adds traceparent to Keycloak requests, causing CORS failures.
  // Use the manual traceInterceptor for backend API requests instead.
}
