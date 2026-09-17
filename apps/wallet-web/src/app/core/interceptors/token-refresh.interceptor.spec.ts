import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { tokenRefreshInterceptor } from './token-refresh.interceptor';
import { AuthService } from '@core/infrastructure/auth.service';

function createValidToken(): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const futureExp = Math.floor(Date.now() / 1000) + 3600;
  const payload = btoa(JSON.stringify({ sub: 'user-1', exp: futureExp, iat: futureExp - 3600 }));
  return `${header}.${payload}.sig`;
}

describe('tokenRefreshInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([tokenRefreshInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should pass through non-401 responses', () => {
    auth.setToken(createValidToken());

    http.get('/api/test').subscribe((res) => {
      expect(res).toEqual({ data: 'ok' });
    });

    const req = httpMock.expectOne('/api/test');
    expect(req.request.headers.get('Authorization')).toBe(`Bearer ${auth.getToken()}`);
    req.flush({ data: 'ok' });
  });

  it('should skip refresh for /auth/refresh endpoint', () => {
    http.post('/api/v1/auth/refresh', null).subscribe();

    const req = httpMock.expectOne('/api/v1/auth/refresh');
    expect(req.request.body).toBeNull();
    req.flush({ access_token: 'new' });
  });

  it('should attach access token to /auth/revoke', () => {
    auth.setToken(createValidToken());

    http.post('/api/v1/auth/revoke', {}).subscribe();

    const req = httpMock.expectOne('/api/v1/auth/revoke');
    expect(req.request.headers.get('Authorization')).toBe(`Bearer ${auth.getToken()}`);
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('should attempt refresh on 401 and retry original request', () => {
    auth.setToken(createValidToken());

    http.get('/api/protected').subscribe((res) => {
      expect(res).toEqual({ data: 'retry-ok' });
    });

    // First request fails with 401
    const req1 = httpMock.expectOne('/api/protected');
    req1.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    // Refresh call — null body, withCredentials for HttpOnly cookie
    const refreshReq = httpMock.expectOne('/api/v1/auth/refresh');
    expect(refreshReq.request.body).toBeNull();
    expect(refreshReq.request.withCredentials).toBe(true);
    refreshReq.flush({ access_token: 'new-token', refresh_token: 'new-refresh', expires_in: 300 });

    // Retried original request with new access token
    const req2 = httpMock.expectOne('/api/protected');
    expect(req2.request.headers.get('Authorization')).toBe('Bearer new-token');
    req2.flush({ data: 'retry-ok' });
  });

  it('should clear token on refresh failure', () => {
    auth.setToken(createValidToken());

    http.get('/api/protected').subscribe({
      error: (err: HttpErrorResponse) => {
        expect(err.status).toBe(401);
      },
    });

    const req1 = httpMock.expectOne('/api/protected');
    req1.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    const refreshReq = httpMock.expectOne('/api/v1/auth/refresh');
    refreshReq.flush('Invalid', { status: 401, statusText: 'Unauthorized' });

    expect(auth.getToken()).toBeNull();
  });

  it('should handle pre-emptive refresh when token is expiring soon', () => {
    // Set a token that expires in 30 seconds (below 60s threshold)
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const exp = Math.floor(Date.now() / 1000) + 30;
    const payload = btoa(JSON.stringify({ sub: 'user-1', exp, iat: exp - 3600 }));
    auth.setToken(`${header}.${payload}.sig`);

    http.get('/api/test').subscribe((res) => {
      expect(res).toEqual({ data: 'ok' });
    });

    // Pre-emptive refresh is triggered
    const refreshReq = httpMock.expectOne('/api/v1/auth/refresh');
    expect(refreshReq.request.withCredentials).toBe(true);
    refreshReq.flush({ access_token: 'fresh-token', refresh_token: 'fresh-refresh', expires_in: 300 });

    // Original request retried with fresh token
    const req = httpMock.expectOne('/api/test');
    expect(req.request.headers.get('Authorization')).toBe('Bearer fresh-token');
    req.flush({ data: 'ok' });
  });
});
