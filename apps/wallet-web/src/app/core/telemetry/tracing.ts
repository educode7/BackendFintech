/**
 * Initialize OpenTelemetry tracing for the browser.
 *
 * TODO Phase 4: Install @opentelemetry/* deps and implement full tracing.
 * Currently a no-op stub — will set up WebTracerProvider, OTLP exporter,
 * auto-instrumentation, and W3C Trace Context propagation once deps are installed.
 *
 * Call once in app bootstrap (main.ts).
 */
export async function initializeTracing(): Promise<void> {
  console.info('[tracing] OpenTelemetry stub — tracing not yet configured');
}
