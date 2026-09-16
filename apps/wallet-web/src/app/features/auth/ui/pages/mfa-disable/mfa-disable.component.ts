import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '@core/infrastructure/auth.service';
import { NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';

const MFA_API = '/api/v1/auth/mfa';

@Component({
  selector: 'app-mfa-disable',
  standalone: true,
  imports: [NgClass, FormsModule],
  template: `
    <div class="mfa-container">
      <h2>Disable Two-Factor Authentication</h2>

      @if (success) {
        <div class="success-message">
          <p>✅ MFA has been disabled. Redirecting to settings...</p>
        </div>
      } @else {
        <div class="warning-box">
          <p>⚠️ Disabling MFA will make your account less secure. You will no longer need a verification code to sign in.</p>
        </div>

        <div class="verify-section">
          <label>Enter your current TOTP code to confirm:</label>
          <input
            type="text"
            [(ngModel)]="code"
            maxlength="6"
            pattern="[0-9]{6}"
            placeholder="000000"
            class="code-input"
            [class.input-error]="!!errorMessage"
            (input)="errorMessage = ''"
            autofocus
          />
          @if (errorMessage) {
            <div class="field-error">{{ errorMessage }}</div>
          }
          <button class="btn btn-danger" (click)="disableMfa()" [disabled]="code.length !== 6 || submitting">
            {{ submitting ? 'Disabling...' : 'Disable MFA' }}
          </button>
          <button class="btn btn-secondary" (click)="cancel()">Cancel</button>
        </div>
      }
    </div>
  `,
  styles: [`
    .mfa-container { max-width: 400px; margin: 2rem auto; padding: 1.5rem; }
    h2 { margin-bottom: 1rem; color: #dc2626; }
    .warning-box { background: #fef3c7; border: 1px solid #fbbf24; border-radius: 0.5rem; padding: 1rem; margin-bottom: 1.5rem; }
    .warning-box p { color: #92400e; font-size: 0.9rem; }
    .success-message { text-align: center; padding: 2rem; background: #f0fdf4; border: 1px solid #86efac; border-radius: 0.5rem; }
    .success-message p { color: #166534; font-weight: 500; }
    .verify-section { display: flex; flex-direction: column; gap: 0.75rem; }
    .verify-section label { color: #374151; font-weight: 500; }
    .code-input { font-size: 1.5rem; letter-spacing: 0.3em; text-align: center; padding: 0.75rem; border: 2px solid #d1d5db; border-radius: 0.5rem; width: 100%; font-family: monospace; }
    .code-input:focus { border-color: #dc2626; outline: none; }
    .input-error { border-color: #dc2626; }
    .field-error { color: #dc2626; font-size: 0.875rem; }
    .btn { padding: 0.75rem 1.5rem; border: none; border-radius: 0.5rem; cursor: pointer; font-size: 1rem; }
    .btn-danger { background: #dc2626; color: white; }
    .btn-danger:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-secondary { background: #e5e7eb; color: #374151; margin-top: 0.5rem; }
  `],
})
export class MfaDisableComponent {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  code = '';
  errorMessage = '';
  submitting = false;
  success = false;

  disableMfa(): void {
    if (this.code.length !== 6) return;

    this.submitting = true;
    this.errorMessage = '';

    const headers = new HttpHeaders({ Authorization: `Bearer ${this.auth.getToken()}` });

    this.http
      .request('POST', `${MFA_API}/disable`, { body: { code: this.code }, headers, observe: 'response' })
      .subscribe({
        next: (res) => {
          this.submitting = false;
          if (res.status === 204) {
            this.success = true;
            setTimeout(() => this.router.navigate(['/']), 3000);
          }
        },
        error: (err) => {
          this.submitting = false;
          this.errorMessage = err.error?.message || 'Failed to disable MFA. Check your code.';
        },
      });
  }

  cancel(): void {
    this.router.navigate(['/']);
  }
}
