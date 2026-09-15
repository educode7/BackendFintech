import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { requestIdInterceptor } from './request-id.interceptor';

describe('requestIdInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([requestIdInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should add X-Request-Id header', () => {
    httpClient.get('/api/test').subscribe();

    const req = httpMock.expectOne('/api/test');
    expect(req.request.headers.has('X-Request-Id')).toBe(true);
    expect(req.request.headers.get('X-Request-Id')).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/
    );
    req.flush({});
  });

  it('should add X-Client-Version header', () => {
    httpClient.get('/api/test').subscribe();

    const req = httpMock.expectOne('/api/test');
    expect(req.request.headers.get('X-Client-Version')).toBe('0.0.0');
    req.flush({});
  });

  it('should add X-Client-Platform header', () => {
    httpClient.get('/api/test').subscribe();

    const req = httpMock.expectOne('/api/test');
    expect(req.request.headers.get('X-Client-Platform')).toBe('web');
    req.flush({});
  });

  it('should preserve existing X-Request-Id', () => {
    const existingId = 'test-existing-id';
    httpClient.get('/api/test', { headers: { 'X-Request-Id': existingId } }).subscribe();

    const req = httpMock.expectOne('/api/test');
    expect(req.request.headers.get('X-Request-Id')).toBe(existingId);
    req.flush({});
  });
});
