import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { OAuthService } from 'angular-oauth2-oidc';
import { Router } from '@angular/router';
import { vi } from 'vitest';

function createOAuthSpy() {
  return {
    configure: vi.fn(),
    loadDiscoveryDocumentAndTryLogin: vi.fn().mockResolvedValue(true),
    hasValidAccessToken: vi.fn().mockReturnValue(false),
    getAccessToken: vi.fn().mockReturnValue(null),
    getAccessTokenExpiration: vi.fn().mockReturnValue(undefined),
    getIdentityClaims: vi.fn().mockReturnValue(null),
    initLoginFlow: vi.fn(),
    logOut: vi.fn(),
  };
}

function createRouterSpy() {
  return { navigate: vi.fn() };
}

describe('AuthService', () => {
  let service: AuthService;
  let oauthService: ReturnType<typeof createOAuthSpy>;
  let router: ReturnType<typeof createRouterSpy>;

  beforeEach(() => {
    oauthService = createOAuthSpy();
    router = createRouterSpy();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: OAuthService, useValue: oauthService },
        { provide: Router, useValue: router },
      ],
    });
    service = TestBed.inject(AuthService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should start unauthenticated when no valid token', () => {
    oauthService.hasValidAccessToken.mockReturnValue(false);
    expect(service.isAuthenticated()).toBe(false);
    expect(service.getToken()).toBeNull();
  });

  it('should return access token from OAuthService', () => {
    oauthService.getAccessToken.mockReturnValue('mock-access-token');
    expect(service.getToken()).toBe('mock-access-token');
  });

  it('should report authenticated when OAuthService has valid token', () => {
    oauthService.hasValidAccessToken.mockReturnValue(true);
    expect(service.isAuthenticated()).toBe(true);
  });

  it('should report not authenticated when OAuthService has no valid token', () => {
    oauthService.hasValidAccessToken.mockReturnValue(false);
    expect(service.isAuthenticated()).toBe(false);
  });

  it('should return null user info when no identity claims', () => {
    oauthService.getIdentityClaims.mockReturnValue(null);
    expect(service.getUserInfo()).toBeNull();
  });

  it('should return user info from identity claims', () => {
    const futureExp = Math.floor(Date.now() / 1000) + 3600;
    oauthService.getIdentityClaims.mockReturnValue({
      sub: 'user-1',
      email: 'test@example.com',
      realm_access: { roles: ['user', 'admin'] },
    });
    oauthService.getAccessTokenExpiration.mockReturnValue(futureExp * 1000);

    const info = service.getUserInfo();
    expect(info).toEqual({
      sub: 'user-1',
      email: 'test@example.com',
      roles: ['user', 'admin'],
      expiresAt: futureExp,
    });
  });

  it('should cache user info after first call', () => {
    const futureExp = Math.floor(Date.now() / 1000) + 3600;
    oauthService.getIdentityClaims.mockReturnValue({
      sub: 'user-1',
      email: 'test@example.com',
      realm_access: { roles: ['user'] },
    });
    oauthService.getAccessTokenExpiration.mockReturnValue(futureExp * 1000);

    const info1 = service.getUserInfo();
    const info2 = service.getCachedUserInfo();
    expect(info1).toEqual(info2);
  });

  it('should return null cached user info initially', () => {
    expect(service.getCachedUserInfo()).toBeNull();
  });

  it('should call login via OAuthService initLoginFlow', () => {
    service.login();
    expect(oauthService.initLoginFlow).toHaveBeenCalled();
  });

  it('should logout and navigate to /login', () => {
    oauthService.getIdentityClaims.mockReturnValue({
      sub: 'u',
      email: 'e@e.com',
      realm_access: { roles: [] },
    });
    oauthService.getAccessTokenExpiration.mockReturnValue(Date.now() + 3600000);
    service.getUserInfo(); // populate cache

    service.logout();
    expect(oauthService.logOut).toHaveBeenCalled();
    expect(service.getCachedUserInfo()).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should detect token expiring soon', () => {
    const expiringMs = Date.now() + 30 * 1000; // 30 seconds from now
    oauthService.getAccessTokenExpiration.mockReturnValue(expiringMs);
    expect(service.isTokenExpiringSoon(60)).toBe(true);
  });

  it('should not report token expiring when far from expiry', () => {
    const farFutureMs = Date.now() + 3600 * 1000; // 1 hour from now
    oauthService.getAccessTokenExpiration.mockReturnValue(farFutureMs);
    expect(service.isTokenExpiringSoon(60)).toBe(false);
  });

  it('should return false for isTokenExpiringSoon when no expiration', () => {
    oauthService.getAccessTokenExpiration.mockReturnValue(undefined);
    expect(service.isTokenExpiringSoon(60)).toBe(false);
  });
});
