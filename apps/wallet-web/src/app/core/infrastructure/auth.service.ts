import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap, catchError, shareReplay } from 'rxjs';

export interface UserInfo {
  sub: string;
  email: string;
  roles: string[];
  expiresAt: number;
}

interface RefreshResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
}

const AUTH_SERVICE_URL = '/api/v1/auth';

/**
 * Auth service — manages access tokens and user identity.
 *
 * Token strategy:
 * - Access token: in-memory only (lost on page reload — user re-authenticates)
 * - Refresh token: HttpOnly Secure SameSite=Strict cookie (managed by backend)
 *
 * Identity: fetched from GET /auth/me (frontend cannot decode JWT).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  /** In-memory access token */
  private token: string | null = null;

  /** Cached user identity (null = not fetched yet) */
  private userInfo: UserInfo | null = null;

  /** In-flight refresh request (coalesces concurrent calls) */
  private refreshInProgress$: Observable<RefreshResponse> | null = null;

  // ─── Token management ────────────────────────────────────

  setToken(token: string): void {
    this.token = token;
  }

  getToken(): string | null {
    return this.token;
  }

  clearToken(): void {
    this.token = null;
    this.userInfo = null;
  }

  /**
   * Check if the user is authenticated.
   * Uses cached user identity when available, falls back to local JWT expiry check.
   */
  isAuthenticated(): boolean {
    // Fast path: cached identity exists and access token is present
    if (this.userInfo && this.token) {
      return this.userInfo.expiresAt * 1000 > Date.now();
    }

    // Fallback: decode JWT locally (before /auth/me has been called)
    if (!this.token) return false;
    return !this.isTokenExpired(this.token);
  }

  // ─── User identity ───────────────────────────────────────

  /**
   * Fetch the current user's identity from the backend.
   * The access token is attached automatically by the auth interceptor.
   */
  getUserInfo(): Observable<UserInfo> {
    if (this.userInfo) {
      return of(this.userInfo);
    }

    return this.http.get<UserInfo>(`${AUTH_SERVICE_URL}/me`).pipe(
      tap((info) => (this.userInfo = info)),
      catchError((err) => {
        // 401 means the access token is invalid — clear everything
        if (err.status === 401) {
          this.clearToken();
        }
        throw err;
      })
    );
  }

  /** Get cached user info without making an HTTP call */
  getCachedUserInfo(): UserInfo | null {
    return this.userInfo;
  }

  // ─── Token expiry helpers ────────────────────────────────

  isTokenExpiringSoon(thresholdSeconds: number): boolean {
    if (!this.token) return false;
    try {
      const payload = this.decodeJwtPayload(this.token);
      if (!payload?.exp) return false;
      const nowSeconds = Math.floor(Date.now() / 1000);
      return payload.exp - nowSeconds <= thresholdSeconds;
    } catch {
      return false;
    }
  }

  /**
   * Refresh the access token using the HttpOnly refresh_token cookie.
   *
   * The cookie is sent automatically by the browser — no need to read it from
   * sessionStorage or include it in the request body.
   *
   * Coalesces concurrent refresh attempts into a single request.
   */
  refreshAccessToken(): Observable<RefreshResponse> {
    if (this.refreshInProgress$) {
      return this.refreshInProgress$;
    }

    this.refreshInProgress$ = this.http
      .post<RefreshResponse>(`${AUTH_SERVICE_URL}/refresh`, null, {
        withCredentials: true,
      })
      .pipe(
        tap({
          next: (response) => {
            this.setToken(response.access_token);
            // Note: new refresh_token cookie is set by the backend via Set-Cookie header
          },
          error: () => {
            this.clearToken();
          },
          complete: () => {
            this.refreshInProgress$ = null;
          },
        }),
        // shareReplay makes the Observable hot — concurrent subscribers share one HTTP request
        shareReplay({ bufferSize: 1, refCount: false }),
      );

    return this.refreshInProgress$;
  }

  // ─── Private helpers ─────────────────────────────────────

  private isTokenExpired(token: string): boolean {
    try {
      const payload = this.decodeJwtPayload(token);
      if (!payload?.exp) return true;
      const nowSeconds = Math.floor(Date.now() / 1000);
      return payload.exp <= nowSeconds;
    } catch {
      return true;
    }
  }

  private decodeJwtPayload(token: string): { exp?: number; sub?: string } | null {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      const payload = atob(parts[1]);
      return JSON.parse(payload);
    } catch {
      return null;
    }
  }
}
