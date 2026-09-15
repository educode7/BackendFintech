import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { idempotencyInterceptor } from './idempotency.interceptor';

describe('idempotencyInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([idempotencyInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should add Idempotency-Key header to POST requests', () => {
    httpClient.post('/api/v1/payments', {}).subscribe();

    const req = httpMock.expectOne('/api/v1/payments');
    expect(req.request.headers.has('Idempotency-Key')).toBe(true);
    expect(req.request.headers.get('Idempotency-Key')).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/
    );
    req.flush({});
  });

  it('should add Idempotency-Key header to PUT requests', () => {
    httpClient.put('/api/v1/accounts/1', {}).subscribe();

    const req = httpMock.expectOne('/api/v1/accounts/1');
    expect(req.request.headers.has('Idempotency-Key')).toBe(true);
    req.flush({});
  });

  it('should add Idempotency-Key header to PATCH requests', () => {
    httpClient.patch('/api/v1/accounts/1', {}).subscribe();

    const req = httpMock.expectOne('/api/v1/accounts/1');
    expect(req.request.headers.has('Idempotency-Key')).toBe(true);
    req.flush({});
  });

  it('should NOT add Idempotency-Key to GET requests', () => {
    httpClient.get('/api/v1/accounts').subscribe();

    const req = httpMock.expectOne('/api/v1/accounts');
    expect(req.request.headers.has('Idempotency-Key')).toBe(false);
    req.flush([]);
  });

  it('should NOT add Idempotency-Key to DELETE requests', () => {
    httpClient.delete('/api/v1/accounts/1').subscribe();

    const req = httpMock.expectOne('/api/v1/accounts/1');
    expect(req.request.headers.has('Idempotency-Key')).toBe(false);
    req.flush({});
  });
});
