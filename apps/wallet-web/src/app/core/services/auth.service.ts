import { Injectable } from '@angular/core';

interface JwtPayload {
  sub: string;
  exp: number;
  iat: number;
  [key: string]: unknown;
}

/**
 * In-memory auth token service.
 * Tokens are stored in memory only — NEVER in localStorage/ sessionStorage.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private token: string | null = null;

  setToken(token: string): void {
    this.token = token;
  }

  getToken(): string | null {
    return this.token;
  }

  clearToken(): void {
    this.token = null;
  }

  isAuthenticated(): boolean {
    if (!this.token) return false;
    return !this.isTokenExpired(this.token);
  }

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
