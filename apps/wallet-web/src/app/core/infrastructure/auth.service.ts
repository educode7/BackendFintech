import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

interface JwtPayload {
  sub: string;
  exp: number;
  iat: number;
  [key: string]: unknown;
}

interface RefreshResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
}

const REFRESH_TOKEN_KEY = 'wallet_refresh_token';
const AUTH_SERVICE_URL = '/api/v1/auth';

/**
 * In-memory auth token service.
 * Access tokens are stored in memory only.
 * Refresh tokens are stored in sessionStorage for cross-reload persistence.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private token: string | null = null;
  private refreshInProgress$: Observable<RefreshResponse> | null = null;

  constructor(private http: HttpClient) {}

  setToken(token: string): void {
    this.token = token;
  }

  getToken(): string | null {
    return this.token;
  }

  clearToken(): void {
    this.token = null;
    this.clearRefreshToken();
  }

  isAuthenticated(): boolean {
    if (!this.token) return false;
    return !this.isTokenExpired(this.token);
  }

  // --- Refresh token methods ---

  setRefreshToken(token: string): void {
    sessionStorage.setItem(REFRESH_TOKEN_KEY, token);
  }

  getRefreshToken(): string | null {
    return sessionStorage.getItem(REFRESH_TOKEN_KEY);
  }

  clearRefreshToken(): void {
    sessionStorage.removeItem(REFRESH_TOKEN_KEY);
  }

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

  refreshAccessToken(): Observable<RefreshResponse> {
    // Coalesce concurrent refresh attempts into a single request
    if (this.refreshInProgress$) {
      return this.refreshInProgress$;
    }

    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    this.refreshInProgress$ = this.http
      .post<RefreshResponse>(`${AUTH_SERVICE_URL}/refresh`, { refresh_token: refreshToken })
      .pipe(
        tap({
          next: (response) => {
            this.setToken(response.access_token);
            this.setRefreshToken(response.refresh_token);
          },
          error: () => {
            this.clearToken();
          },
          complete: () => {
            this.refreshInProgress$ = null;
          },
        })
      );

    return this.refreshInProgress$;
  }

  // --- Private helpers ---

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

  private decodeJwtPayload(token: string): JwtPayload | null {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      const payload = atob(parts[1]);
      return JSON.parse(payload) as JwtPayload;
    } catch {
      return null;
    }
  }
}
