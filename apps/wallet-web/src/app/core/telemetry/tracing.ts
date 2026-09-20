import {
  CompositePropagator,
  W3CBaggagePropagator,
  W3CTraceContextPropagator,
} from '@opentelemetry/core';
import { WebTracerProvider } from '@opentelemetry/sdk-trace-web';
import { SimpleSpanProcessor, ConsoleSpanExporter } from '@opentelemetry/sdk-trace-base';
import { resourceFromAttributes } from '@opentelemetry/resources';
import { ATTR_SERVICE_NAME, ATTR_SERVICE_VERSION } from '@opentelemetry/semantic-conventions';
import { registerInstrumentations } from '@opentelemetry/instrumentation';
import { getWebAutoInstrumentations } from '@opentelemetry/auto-instrumentations-web';
import { environment } from '@env/environment';

/**
 * Initialize OpenTelemetry tracing for the browser.
 *
 * - Development: ConsoleSpanExporter only (visible in DevTools, no collector needed)
 * - Production: OTLP exporter + ConsoleSpanExporter
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

  registerInstrumentations({
    tracerProvider: provider,
    instrumentations: [
      getWebAutoInstrumentations({
        '@opentelemetry/instrumentation-fetch': {
          propagateTraceHeaderCorsUrls: /.*/,
          clearTimingResources: true,
        },
        '@opentelemetry/instrumentation-xml-http-request': {
          propagateTraceHeaderCorsUrls: /.*/,
          clearTimingResources: true,
        },
      }),
    ],
  });
}
