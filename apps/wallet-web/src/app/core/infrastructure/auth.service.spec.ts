import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

function createExpiredToken(): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = btoa(JSON.stringify({ sub: 'user-1', exp: 1000000000, iat: 1000000000 }));
  const sig = 'fake-signature';
  return `${header}.${payload}.${sig}`;
}

function createValidToken(): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const futureExp = Math.floor(Date.now() / 1000) + 3600;
  const payload = btoa(JSON.stringify({ sub: 'user-1', exp: futureExp, iat: futureExp - 3600 }));
  const sig = 'fake-signature';
  return `${header}.${payload}.${sig}`;
}

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should start unauthenticated', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.getToken()).toBeNull();
  });

  it('should store and retrieve token', () => {
    const token = createValidToken();
    service.setToken(token);
    expect(service.getToken()).toBe(token);
  });

  it('should authenticate with valid token', () => {
    service.setToken(createValidToken());
    expect(service.isAuthenticated()).toBe(true);
  });

  it('should reject expired token', () => {
    service.setToken(createExpiredToken());
    expect(service.isAuthenticated()).toBe(false);
  });

  it('should clear token and user info', () => {
    service.setToken(createValidToken());
    service.clearToken();
    expect(service.getToken()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.getCachedUserInfo()).toBeNull();
  });

  it('should reject malformed token', () => {
    service.setToken('not-a-jwt');
    expect(service.isAuthenticated()).toBe(false);
  });

  it('should fetch user info from /auth/me', () => {
    const mockUserInfo = {
      sub: 'user-1',
      email: 'test@example.com',
      roles: ['user'],
      expiresAt: Math.floor(Date.now() / 1000) + 3600,
    };

    service.getUserInfo().subscribe((info) => {
      expect(info).toEqual(mockUserInfo);
      expect(service.getCachedUserInfo()).toEqual(mockUserInfo);
    });

    const req = httpMock.expectOne('/api/v1/auth/me');
    expect(req.request.method).toBe('GET');
    req.flush(mockUserInfo);
  });

  it('should clear token on 401 from /auth/me', () => {
    service.setToken(createValidToken());

    service.getUserInfo().subscribe({
      error: (err) => {
        expect(err.status).toBe(401);
        expect(service.getToken()).toBeNull();
      },
    });

    const req = httpMock.expectOne('/api/v1/auth/me');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
  });

  it('should refresh access token using HttpOnly cookie', () => {
    const mockRefreshResponse = {
      access_token: createValidToken(),
      refresh_token: 'new-refresh-token',
      expires_in: 300,
    };

    service.refreshAccessToken().subscribe((response) => {
      expect(response.access_token).toBeTruthy();
      expect(service.getToken()).toBe(response.access_token);
    });

    const req = httpMock.expectOne('/api/v1/auth/refresh');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull(); // No body — cookie is automatic
    expect(req.request.withCredentials).toBe(true);
    req.flush(mockRefreshResponse);
  });

  it('should coalesce concurrent refresh calls', () => {
    const mockRefreshResponse = {
      access_token: createValidToken(),
      refresh_token: 'new-refresh-token',
      expires_in: 300,
    };

    // Fire two concurrent refresh calls (both subscribe to the same Observable)
    let result1: any = null;
    let result2: any = null;
    service.refreshAccessToken().subscribe((r) => (result1 = r));
    service.refreshAccessToken().subscribe((r) => (result2 = r));

    // Only one HTTP request should be made
    const req = httpMock.expectOne('/api/v1/auth/refresh');
    expect(req.request.body).toBeNull();
    req.flush(mockRefreshResponse);

    expect(result1).toBeTruthy();
    expect(result2).toBeTruthy();
    expect(result1.access_token).toBe(result2.access_token);
  });
});
