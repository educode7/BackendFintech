import {
  CompositePropagator,
  W3CBaggagePropagator,
  W3CTraceContextPropagator,
} from '@opentelemetry/core';
import { WebTracerProvider } from '@opentelemetry/sdk-trace-web';
import { SimpleSpanProcessor, ConsoleSpanExporter } from '@opentelemetry/sdk-trace-base';
import { resourceFromAttributes } from '@opentelemetry/resources';
import { ATTR_SERVICE_NAME, ATTR_SERVICE_VERSION } from '@opentelemetry/semantic-conventions';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
import { registerInstrumentations } from '@opentelemetry/instrumentation';
import { getWebAutoInstrumentations } from '@opentelemetry/auto-instrumentations-web';

/**
 * Initialize OpenTelemetry tracing for the browser.
 *
 * Angular 22 is zoneless — no Zone.js context manager needed.
 * Uses default context management (ROOT_CONTEXT) for span association.
 * Auto-instrumentation handles fetch/XHR span creation and propagation.
 *
 * Sets up:
 * - WebTracerProvider with W3C Trace Context propagation
 * - OTLP exporter (configurable via OTEL_EXPORTER_OTLP_ENDPOINT)
 * - Auto-instrumentation for fetch/XHR
 *
 * Call once in app bootstrap (main.ts).
 */
export async function initializeTracing(): Promise<void> {
  const endpoint =
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (globalThis as any).env?.['OTEL_EXPORTER_OTLP_ENDPOINT'] ?? 'http://localhost:4318';

  const resource = resourceFromAttributes({
    [ATTR_SERVICE_NAME]: 'wallet-web',
    [ATTR_SERVICE_VERSION]: '0.1.0',
  });

  // OTLP exporter for production — sends to collector
  const otlpExporter = new OTLPTraceExporter({
    url: `${endpoint}/v1/traces`,
  });

  // Console exporter for development — visible in browser DevTools
  const consoleExporter = new ConsoleSpanExporter();

  const provider = new WebTracerProvider({
    resource,
    spanProcessors: [
      new SimpleSpanProcessor(otlpExporter),
      new SimpleSpanProcessor(consoleExporter),
    ],
  });

  // Register with W3C Trace Context propagation (no Zone.js needed)
  provider.register({
    propagator: new CompositePropagator({
      propagators: [
        new W3CBaggagePropagator(),
        new W3CTraceContextPropagator(),
      ],
    }),
  });

  // Auto-instrument fetch and XHR
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

  console.info('[tracing] OpenTelemetry initialized — endpoint:', endpoint);
}
