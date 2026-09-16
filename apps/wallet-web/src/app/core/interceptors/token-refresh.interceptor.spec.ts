import { TestBed } from '@angular/core/testing';
import {
  HttpClient,
  HttpErrorResponse,
  HttpHeaders,
  HTTP_INTERCEPTORS,
} from '@angular/common/http';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
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
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        {
          provide: HTTP_INTERCEPTORS,
          useValue: tokenRefreshInterceptor,
          multi: true,
        },
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
    req.flush({ data: 'ok' });
  });

  it('should skip refresh for /auth/refresh endpoint', () => {
    http.post('/api/v1/auth/refresh', {}).subscribe();

    const req = httpMock.expectOne('/api/v1/auth/refresh');
    req.flush({ access_token: 'new' });
  });

  it('should skip refresh for /auth/revoke endpoint', () => {
    http.post('/api/v1/auth/revoke', {}).subscribe();

    const req = httpMock.expectOne('/api/v1/auth/revoke');
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

    // Refresh call
    const refreshReq = httpMock.expectOne('/api/v1/auth/refresh');
    refreshReq.flush({ access_token: 'new-token', refresh_token: 'new-refresh', expires_in: 300 });

    // Retried original request
    const req2 = httpMock.expectOne('/api/protected');
    req2.flush({ data: 'retry-ok' });
  });

  it('should redirect to login on refresh failure', () => {
    auth.setToken(createValidToken());
    const consoleSpy = spyOn(console, 'error');

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
});
