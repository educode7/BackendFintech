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
 * - Always exports spans via OTLP to environment.otelEndpoint (the
 *   host-published otel-collector), so the browser appears in traces in
 *   both development and production.
 * - Also keeps ConsoleSpanExporter (visible in DevTools).
 *
 * Auto-instrumentation is NOT used because it patches fetch/XHR at the
 * browser level and adds traceparent to ALL requests, including Keycloak
 * OIDC requests that have strict CORS policies. The traceInterceptor
 * creates a real CLIENT span per API call and handles propagation instead.
 *
 * Call once in app bootstrap (main.ts).
 */
export async function initializeTracing(): Promise<void> {
  const resource = resourceFromAttributes({
    [ATTR_SERVICE_NAME]: 'wallet-web',
    [ATTR_SERVICE_VERSION]: '0.1.0',
  });

  const spanProcessors = [new SimpleSpanProcessor(new ConsoleSpanExporter())];

  const { OTLPTraceExporter } = await import('@opentelemetry/exporter-trace-otlp-http');
  spanProcessors.push(
    new SimpleSpanProcessor(
      new OTLPTraceExporter({
        url: environment.otelEndpoint,
      }),
    ),
  );

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
