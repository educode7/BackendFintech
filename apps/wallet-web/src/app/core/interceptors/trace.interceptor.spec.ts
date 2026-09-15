import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { traceInterceptor } from './trace.interceptor';

// Mock OTel API
vi.mock('@opentelemetry/api', () => ({
  trace: {
    getTracer: () => ({}),
    getActiveSpan: () => null,
  },
  context: {
    active: () => ({}),
  },
  propagation: {
    inject: () => {},
  },
}));

describe('traceInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
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

  it('should add traceparent header to requests', () => {
    httpClient.get('/api/test').subscribe();

    const req = httpMock.expectOne('/api/test');
    const traceparent = req.request.headers.get('traceparent');
    expect(traceparent).toBeTruthy();
    req.flush({});
  });

  it('should follow W3C traceparent format: 00-{32hex}-{16hex}-01', () => {
    httpClient.get('/api/test').subscribe();

    const req = httpMock.expectOne('/api/test');
    const traceparent = req.request.headers.get('traceparent')!;
    expect(traceparent).toMatch(/^00-[0-9a-f]{32}-[0-9a-f]{16}-01$/);
    req.flush({});
  });

  it('should generate unique trace IDs per request', () => {
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

  it('should not overwrite existing traceparent header', () => {
    const existing = '00-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa-bbbbbbbbcccccccc-01';
    httpClient.get('/api/test', { headers: { traceparent: existing } }).subscribe();

    const req = httpMock.expectOne('/api/test');
    expect(req.request.headers.get('traceparent')).toBe(existing);
    req.flush({});
  });
});
