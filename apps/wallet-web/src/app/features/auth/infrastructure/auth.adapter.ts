import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { BaseAdapter } from '@core/infrastructure/base.adapter';
import { AuthService } from '@core/infrastructure/auth.service';
import type {
  MfaSetupResponse,
  MfaVerifyResponse,
} from '../domain/mfa.model';

/**
 * Auth HTTP adapter — consumes /api/v1/auth/mfa.
 */
@Injectable({ providedIn: 'root' })
export class AuthAdapter extends BaseAdapter {
  private readonly authHttp = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly authBaseUrl = `${this.baseUrl}/api/v1/auth/mfa`;

  setup(): Observable<MfaSetupResponse> {
    const token = this.auth.getToken();
    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`,
    });

    return this.authHttp
      .post<MfaSetupResponse>(`${this.authBaseUrl}/setup`, {}, { headers })
      .pipe(catchError(this.handleError));
  }

  verify(code: string): Observable<MfaVerifyResponse> {
    const token = this.auth.getToken();
    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`,
    });

    return this.authHttp
      .post<MfaVerifyResponse>(
        `${this.authBaseUrl}/verify`,
        { code },
        { headers },
      )
      .pipe(catchError(this.handleError));
  }

  disable(code: string): Observable<void> {
    const token = this.auth.getToken();
    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`,
    });

    return this.authHttp
      .post<void>(`${this.authBaseUrl}/disable`, { code }, { headers })
      .pipe(catchError(this.handleError));
  }
}
