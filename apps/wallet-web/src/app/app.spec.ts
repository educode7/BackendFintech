import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { App } from './app';
import { OAuthService } from 'angular-oauth2-oidc';
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

describe('App', () => {
  let oauthService: ReturnType<typeof createOAuthSpy>;

  beforeEach(async () => {
    oauthService = createOAuthSpy();

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        { provide: OAuthService, useValue: oauthService },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should show login page when not authenticated', () => {
    oauthService.hasValidAccessToken.mockReturnValue(false);
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    // Login page renders without sidebar
    expect(compiled.querySelector('.sidebar')).toBeNull();
  });

  it('should show sidebar when authenticated', () => {
    oauthService.hasValidAccessToken.mockReturnValue(true);
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.sidebar')).toBeTruthy();
    expect(compiled.querySelector('.nav-list')).toBeTruthy();
  });

  it('should have nav links for all features when authenticated', () => {
    oauthService.hasValidAccessToken.mockReturnValue(true);
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const links = fixture.nativeElement.querySelectorAll('.nav-list a');
    expect(links.length).toBe(4);
  });
});
