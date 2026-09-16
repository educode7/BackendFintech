import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '@core/infrastructure/auth.service';
import { MfaVerifyResponse } from '../../../domain/mfa.model';
import { NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';

const MFA_API = '/api/v1/auth/mfa';
const MAX_ATTEMPTS = 5;
const LOCKOUT_SECONDS = 300;

@Component({
  selector: 'app-mfa-login',
  standalone: true,
  imports: [NgClass, FormsModule],
  template: `
    <div class="mfa-container">
      <h2>Two-Factor Verification</h2>
      <p class="instruction">Enter the 6-digit code from your authenticator app.</p>

      @if (locked) {
        <div class="lockout">
          <p>Too many failed attempts. Please wait {{ formatTime(remainingSeconds) }}.</p>
        </div>
      } @else {
        <div class="verify-section">
          <input
            type="text"
            [(ngModel)]="code"
            maxlength="6"
            pattern="[0-9]{6}"
            placeholder="000000"
            class="code-input"
            [class.input-error]="!!errorMessage"
            (input)="errorMessage = ''"
            [disabled]="locked"
            autofocus
          />
          @if (errorMessage) {
            <div class="field-error">{{ errorMessage }}</div>
          }
          <div class="attempts-info">
            {{ MAX_ATTEMPTS - attempts }} attempts remaining before lockout
          </div>
          <button
            class="btn btn-primary"
            (click)="verify()"
            [disabled]="code.length !== 6 || verifying"
          >
            {{ verifying ? 'Verifying...' : 'Verify' }}
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    .mfa-container { max-width: 400px; margin: 4rem auto; padding: 1.5rem; text-align: center; }
    h2 { margin-bottom: 0.5rem; }
    .instruction { color: #6b7280; margin-bottom: 2rem; }
    .verify-section { display: flex; flex-direction: column; gap: 0.75rem; }
    .code-input { font-size: 2rem; letter-spacing: 0.4em; text-align: center; padding: 0.75rem; border: 2px solid #d1d5db; border-radius: 0.5rem; width: 100%; font-family: monospace; }
    .code-input:focus { border-color: #3b82f6; outline: none; }
    .input-error { border-color: #dc2626; }
    .field-error { color: #dc2626; font-size: 0.875rem; }
    .attempts-info { color: #9ca3af; font-size: 0.8rem; }
    .btn { padding: 0.75rem 1.5rem; border: none; border-radius: 0.5rem; cursor: pointer; font-size: 1rem; }
    .btn-primary { background: #3b82f6; color: white; }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
    .lockout { background: #fef3c7; border: 1px solid #fbbf24; border-radius: 0.5rem; padding: 1.5rem; margin-top: 1rem; }
    .lockout p { color: #92400e; font-weight: 500; }
  `],
})
export class MfaLoginComponent implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly MAX_ATTEMPTS = MAX_ATTEMPTS;

  code = '';
  errorMessage = '';
  verifying = false;
  attempts = 0;
  locked = false;
  remainingSeconds = 0;
  private timerInterval: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.startLockoutIfNeeded();
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  verify(): void {
    if (this.code.length !== 6 || this.locked) return;

    this.verifying = true;
    this.errorMessage = '';

    const headers = new HttpHeaders({ Authorization: `Bearer ${this.auth.getToken()}` });

    this.http
      .post<MfaVerifyResponse>(`${MFA_API}/verify`, { code: this.code }, { headers })
      .subscribe({
        next: (res) => {
          this.verifying = false;
          if (res.verified) {
            this.router.navigate(['/']);
          } else {
            this.handleFailure('Invalid code. Please try again.');
          }
        },
        error: (err) => {
          this.verifying = false;
          this.handleFailure(err.error?.message || 'Verification failed.');
        },
      });
  }

  formatTime(seconds: number): string {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  private handleFailure(message: string): void {
    this.attempts++;
    this.errorMessage = message;
    this.code = '';

    if (this.attempts >= MAX_ATTEMPTS) {
      this.locked = true;
      this.remainingSeconds = LOCKOUT_SECONDS;
      this.startTimer();
    }
  }

  private startTimer(): void {
    this.timerInterval = setInterval(() => {
      this.remainingSeconds--;
      if (this.remainingSeconds <= 0) {
        if (this.timerInterval) clearInterval(this.timerInterval);
        this.locked = false;
        this.attempts = 0;
        this.remainingSeconds = 0;
      }
    }, 1000);
  }

  private startLockoutIfNeeded(): void {
    const lockoutUntil = sessionStorage.getItem('mfa_lockout_until');
    if (lockoutUntil) {
      const remaining = Math.ceil((Number(lockoutUntil) - Date.now()) / 1000);
      if (remaining > 0) {
        this.locked = true;
        this.remainingSeconds = remaining;
        this.attempts = MAX_ATTEMPTS;
        this.startTimer();
      } else {
        sessionStorage.removeItem('mfa_lockout_until');
      }
    }
  }
}
