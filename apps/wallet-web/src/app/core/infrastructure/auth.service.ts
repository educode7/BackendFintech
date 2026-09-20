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
      silentRefreshRedirectUri: `${window.location.origin}/silent-refresh.html`,
      useSilentRefresh: false,
      silentRefreshTimeout: 5000,
      oidc: true,
      strictDiscoveryDocumentValidation: false,
      sessionChecksEnabled: true,
      showDebugInformation: false,
    });

    // Handle the OAuth callback (code exchange)
    await this.oauthService.loadDiscoveryDocumentAndTryLogin();

    // Auto-login if not authenticated
    if (!this.oauthService.hasValidAccessToken()) {
      this.login();
    } else {
      this.setupUserInfo();
    }
  }

  // ─── Login / Logout ─────────────────────────────────────

  login(): void {
    this.oauthService.initLoginFlow();
  }

  logout(): void {
    this.oauthService.logOut();
    this.userInfo = null;
    this.router.navigate(['/login']);
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
