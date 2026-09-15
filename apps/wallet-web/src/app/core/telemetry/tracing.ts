/**
 * OpenTelemetry tracing configuration.
 *
 * Initializes the Web TracerProvider with W3C Trace Context propagation.
 * In production, replace ConsoleSpanExporter with OTLPTraceExporter
 * pointing to your collector endpoint.
 */
export function initializeTracing(): void {
  // Placeholder for OpenTelemetry initialization.
  // When deps are installed, uncomment and configure:
  //
  // import { WebTracerProvider } from '@opentelemetry/sdk-trace-web';
  // import { ConsoleSpanExporter, SimpleSpanProcessor } from '@opentelemetry/sdk-trace-base';
  // import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
  // import { Resource } from '@opentelemetry/resources';
  // import { ATTR_SERVICE_NAME, ATTR_SERVICE_VERSION } from '@opentelemetry/semantic-conventions';
  // import { W3CTraceContextPropagator } from '@opentelemetry/core';
  // import { propagation } from '@opentelemetry/api';
  //
  // const provider = new WebTracerProvider({
  //   resource: new Resource({
  //     [ATTR_SERVICE_NAME]: 'wallet-web',
  //     [ATTR_SERVICE_VERSION]: '0.0.0',
  //   }),
  // });
  //
  // const exporter = new OTLPTraceExporter({
  //   url: 'http://localhost:4318/v1/traces',
  // });
  //
  // provider.addSpanProcessor(new SimpleSpanProcessor(exporter));
  // provider.addSpanProcessor(new SimpleSpanProcessor(new ConsoleSpanExporter()));
  //
  // provider.register();
  // propagation.setGlobalPropagator(new W3CTraceContextPropagator());

  console.info('[tracing] OpenTelemetry tracing placeholder — deps not yet installed');
}
