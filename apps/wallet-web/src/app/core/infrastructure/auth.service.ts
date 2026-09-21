import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { environment } from '@env/environment';

export interface UserInfo {
  sub: string;
  email: string;
  roles: string[];
  expiresAt: number;
}

/**
 * Auth service — manages OIDC login via Keycloak.
 *
 * Uses angular-oauth2-oidc for Authorization Code + PKCE flow.
 * Access tokens are in-memory only. Refresh tokens are HttpOnly cookies
 * managed by the backend auth-service.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oauthService = inject(OAuthService);
  private readonly router = inject(Router);

  /** Cached user identity (null = not fetched yet) */
  private userInfo: UserInfo | null = null;

  // ─── Initialization ─────────────────────────────────────

  /**
   * Called by APP_INITIALIZER before the app renders.
   * Configures OIDC and handles the callback redirect if present.
   */
  async init(): Promise<void> {
    this.oauthService.configure({
      issuer: environment.oidc.issuer,
      clientId: environment.oidc.clientId,
      redirectUri: window.location.origin,
      scope: environment.oidc.scope,
      responseType: 'code',
      oidc: true,
      strictDiscoveryDocumentValidation: false,
      sessionChecksEnabled: false,
      showDebugInformation: true,
      // Silent refresh via iframe — restores session after page refresh
      silentRefreshRedirectUri: `${window.location.origin}/assets/silent-refresh.html`,
      useSilentRefresh: true,
      silentRefreshTimeout: 5000,
      // PKCE is required for public clients
    });

    console.log('[AuthService] URL:', window.location.href);

    // Load discovery document first
    await this.oauthService.loadDiscoveryDocument();

    // Try login from URL params (callback) — returns true if code was exchanged
    const loginResult = await this.oauthService.tryLogin();
    console.log('[AuthService] tryLogin result:', loginResult);

    if (this.oauthService.hasValidAccessToken()) {
      console.log('[AuthService] Token after tryLogin — setting up user info');
      this.setupUserInfo();
      if (loginResult) {
        this.router.navigate(['/dashboard']);
      }
      return;
    }

    // No token from callback — try silent refresh (iframe to Keycloak)
    console.log('[AuthService] No token, attempting silent refresh...');
    try {
      await this.oauthService.silentRefresh();
      console.log('[AuthService] Silent refresh result:', this.oauthService.hasValidAccessToken());
      if (this.oauthService.hasValidAccessToken()) {
        this.setupUserInfo();
      }
    } catch (e) {
      console.log('[AuthService] Silent refresh failed (no active session):', e);
    }
  }

  // ─── Login / Logout ─────────────────────────────────────

  login(): void {
    this.oauthService.initLoginFlow();
  }

  logout(): void {
    this.oauthService.logOut();
    this.userInfo = null;
    this.router.navigate(['/']);
  }

  // ─── Token management ──────────────────────────────────

  /** Get the current access token (for manual use in headers) */
  getToken(): string | null {
    return this.oauthService.getAccessToken();
  }

  /** Check if the user is authenticated */
  isAuthenticated(): boolean {
    return this.oauthService.hasValidAccessToken();
  }

  // ─── User identity ──────────────────────────────────────

  /** Get user info from the decoded ID token */
  getUserInfo(): UserInfo | null {
    if (this.userInfo) {
      return this.userInfo;
    }
    this.setupUserInfo();
    return this.userInfo;
  }

  /** Get cached user info without making an HTTP call */
  getCachedUserInfo(): UserInfo | null {
    return this.userInfo;
  }

  /** Check if access token is expiring soon (within thresholdSeconds) */
  isTokenExpiringSoon(thresholdSeconds: number): boolean {
    const claims = this.oauthService.getAccessTokenExpiration();
    if (!claims) return false;
    const nowMs = Date.now();
    const remaining = claims - nowMs;
    return remaining <= thresholdSeconds * 1000;
  }

  // ─── Private helpers ────────────────────────────────────

  private setupUserInfo(): void {
    const claims = this.oauthService.getAccessTokenExpiration();
    const idClaims = this.oauthService.getIdentityClaims();
    if (!idClaims) return;

    this.userInfo = {
      sub: idClaims['sub'] ?? '',
      email: idClaims['email'] ?? '',
      roles: idClaims['realm_access']?.['roles'] ?? [],
      expiresAt: claims ? Math.floor(claims / 1000) : 0,
    };
  }
}
