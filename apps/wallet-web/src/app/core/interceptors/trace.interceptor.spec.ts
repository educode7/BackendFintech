import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SpanKind, SpanStatusCode } from '@opentelemetry/api';
import { WebTracerProvider } from '@opentelemetry/sdk-trace-web';
import { InMemorySpanExporter, SimpleSpanProcessor } from '@opentelemetry/sdk-trace-base';
import { traceInterceptor } from './trace.interceptor';

// Real OTel API + in-memory exporter: span lifecycle, W3C injection, and
// error status are asserted against actual spans (no API mocks).
const spanExporter = new InMemorySpanExporter();
const provider = new WebTracerProvider({
  spanProcessors: [new SimpleSpanProcessor(spanExporter)],
});
provider.register();

describe('traceInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    spanExporter.reset();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([traceInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('creates a real CLIENT span per API call', () => {
    httpClient.get('/api/test').subscribe();

    const req = httpMock.expectOne('/api/test');
    req.flush({});

    const spans = spanExporter.getFinishedSpans();
    expect(spans).toHaveLength(1);
    expect(spans[0].name).toBe('GET /api/test');
    expect(spans[0].kind).toBe(SpanKind.CLIENT);
    expect(spans[0].attributes['http.request.method']).toBe('GET');
    expect(spans[0].attributes['url.full']).toBe('/api/test');
  });

  it('injects a W3C traceparent matching the created span', () => {
    httpClient.get('/api/test').subscribe();

    const req = httpMock.expectOne('/api/test');
    const traceparent = req.request.headers.get('traceparent');
    expect(traceparent).toMatch(/^00-[0-9a-f]{32}-[0-9a-f]{16}-01$/);

    req.flush({});
    const span = spanExporter.getFinishedSpans()[0];
    expect(traceparent).toBe(
      `00-${span.spanContext().traceId}-${span.spanContext().spanId}-01`,
    );
  });

  it('generates a unique trace ID per request', () => {
    httpClient.get('/api/test1').subscribe();
    const req1 = httpMock.expectOne('/api/test1');
    const trace1 = req1.request.headers.get('traceparent');
    req1.flush({});

    httpClient.get('/api/test2').subscribe();
    const req2 = httpMock.expectOne('/api/test2');
    const trace2 = req2.request.headers.get('traceparent');
    req2.flush({});

    expect(trace1).not.toBe(trace2);
  });

  it('records the exception and sets ERROR status on failure', () => {
    let receivedError: unknown;
    httpClient.get('/api/fail').subscribe({ error: (e) => (receivedError = e) });

    const req = httpMock.expectOne('/api/fail');
    req.flush('boom', { status: 500, statusText: 'Server Error' });
    expect(receivedError).toBeTruthy();

    const spans = spanExporter.getFinishedSpans();
    expect(spans).toHaveLength(1);
    expect(spans[0].status.code).toBe(SpanStatusCode.ERROR);
    expect(spans[0].events.some((e) => e.name === 'exception')).toBe(true);
  });

  it('ends the span even when the request fails', () => {
    httpClient.get('/api/fail').subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/fail');
    req.flush('boom', { status: 500, statusText: 'Server Error' });

    // Only ended spans are exported by SimpleSpanProcessor.
    expect(spanExporter.getFinishedSpans()).toHaveLength(1);
  });

  it('does not overwrite an existing traceparent header', () => {
    const existing = '00-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa-bbbbbbbbbbbbbbbbbbb-01';
    httpClient.get('/api/test', { headers: { traceparent: existing } }).subscribe();

    const req = httpMock.expectOne('/api/test');
    expect(req.request.headers.get('traceparent')).toBe(existing);
    req.flush({});
  });

  it('skips Keycloak/OIDC endpoints without creating a span', () => {
    httpClient
      .post('http://keycloak:8180/realms/wallet/protocol/openid-connect/token', {})
      .subscribe();

    const req = httpMock.expectOne(
      'http://keycloak:8180/realms/wallet/protocol/openid-connect/token',
    );
    expect(req.request.headers.get('traceparent')).toBeNull();
    req.flush({});

    expect(spanExporter.getFinishedSpans()).toHaveLength(0);
  });
});
