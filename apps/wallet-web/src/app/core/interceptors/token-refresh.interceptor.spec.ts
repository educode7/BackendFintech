import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { tokenRefreshInterceptor } from './token-refresh.interceptor';
import { OAuthService } from 'angular-oauth2-oidc';
import { vi } from 'vitest';

function createOAuthSpy() {
  return {
    hasValidAccessToken: vi.fn().mockReturnValue(false),
    getAccessToken: vi.fn().mockReturnValue(null),
    initLoginFlow: vi.fn(),
  };
}

describe('tokenRefreshInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let oauthService: ReturnType<typeof createOAuthSpy>;

  beforeEach(() => {
    oauthService = createOAuthSpy();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([tokenRefreshInterceptor])),
        provideHttpClientTesting(),
        { provide: OAuthService, useValue: oauthService },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should pass through non-401 responses', () => {
    oauthService.hasValidAccessToken.mockReturnValue(true);
    oauthService.getAccessToken.mockReturnValue('valid-token');

    http.get('/api/test').subscribe((res) => {
      expect(res).toEqual({ data: 'ok' });
    });

    const req = httpMock.expectOne('/api/test');
    expect(req.request.headers.get('Authorization')).toBe('Bearer valid-token');
    req.flush({ data: 'ok' });
  });

  it('should attach access token when available', () => {
    oauthService.hasValidAccessToken.mockReturnValue(true);
    oauthService.getAccessToken.mockReturnValue('my-token');

    http.get('/api/test').subscribe();

    const req = httpMock.expectOne('/api/test');
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-token');
    req.flush({});
  });

  it('should not attach Authorization header when no valid token', () => {
    oauthService.hasValidAccessToken.mockReturnValue(false);

    http.get('/api/test').subscribe();

    const req = httpMock.expectOne('/api/test');
    expect(req.request.headers.get('Authorization')).toBeNull();
    req.flush({});
  });

  it('should redirect to Keycloak login on 401', () => {
    oauthService.hasValidAccessToken.mockReturnValue(true);
    oauthService.getAccessToken.mockReturnValue('expired-token');

    http.get('/api/protected').subscribe({
      error: (err: HttpErrorResponse) => {
        expect(err.status).toBe(401);
      },
    });

    const req = httpMock.expectOne('/api/protected');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(oauthService.initLoginFlow).toHaveBeenCalled();
  });

  it('should not redirect on non-401 errors', () => {
    oauthService.hasValidAccessToken.mockReturnValue(true);
    oauthService.getAccessToken.mockReturnValue('valid-token');

    http.get('/api/test').subscribe({
      error: (err: HttpErrorResponse) => {
        expect(err.status).toBe(500);
      },
    });

    const req = httpMock.expectOne('/api/test');
    req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });

    expect(oauthService.initLoginFlow).not.toHaveBeenCalled();
  });

  it('should skip auth header for Keycloak realm URLs', () => {
    oauthService.hasValidAccessToken.mockReturnValue(true);
    oauthService.getAccessToken.mockReturnValue('some-token');

    http.get('http://keycloak:8180/realms/wallet/.well-known/openid-configuration').subscribe();

    const req = httpMock.expectOne('http://keycloak:8180/realms/wallet/.well-known/openid-configuration');
    expect(req.request.headers.get('Authorization')).toBeNull();
    req.flush({});
  });

  it('should skip auth header for Keycloak protocol URLs', () => {
    oauthService.hasValidAccessToken.mockReturnValue(true);
    oauthService.getAccessToken.mockReturnValue('some-token');

    http.get('http://keycloak:8180/realms/wallet/protocol/openid-connect/token').subscribe();

    const req = httpMock.expectOne('http://keycloak:8180/realms/wallet/protocol/openid-connect/token');
    expect(req.request.headers.get('Authorization')).toBeNull();
    req.flush({});
  });

  it('should attach token to POST requests', () => {
    oauthService.hasValidAccessToken.mockReturnValue(true);
    oauthService.getAccessToken.mockReturnValue('post-token');

    http.post('/api/data', { name: 'test' }).subscribe();

    const req = httpMock.expectOne('/api/data');
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.get('Authorization')).toBe('Bearer post-token');
    req.flush({ ok: true });
  });
});
