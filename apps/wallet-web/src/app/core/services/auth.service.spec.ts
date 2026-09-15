import { TestBed } from '@angular/core/testing';
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

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AuthService);
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

  it('should clear token', () => {
    service.setToken(createValidToken());
    service.clearToken();
    expect(service.getToken()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
  });

  it('should reject malformed token', () => {
    service.setToken('not-a-jwt');
    expect(service.isAuthenticated()).toBe(false);
  });
});
