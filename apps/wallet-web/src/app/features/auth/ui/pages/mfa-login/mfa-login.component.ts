import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { AuthStore } from '../../../application/stores/auth.store';
import { FormsModule } from '@angular/forms';

const MAX_ATTEMPTS = 5;

@Component({
  selector: 'app-mfa-login',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="mfa-container">
      <h2>Two-Factor Verification</h2>
      <p class="instruction">Enter the 6-digit code from your authenticator app.</p>

      @if (store.locked()) {
        <div class="lockout">
          <p>Too many failed attempts. Please wait {{ formatTime(store.remainingSeconds()) }}.</p>
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
            [class.input-error]="!!store.verifyError()"
            [attr.aria-invalid]="!!store.verifyError()"
            [attr.aria-describedby]="store.verifyError() ? 'login-error-msg' : null"
            (input)="store.clearVerifyError()"
            [disabled]="store.locked()"
            autofocus
          />
          @if (store.verifyError()) {
            <div id="login-error-msg" class="field-error" role="alert" aria-live="assertive">{{ store.verifyError() }}</div>
          }
          <div class="attempts-info">
            {{ MAX_ATTEMPTS - store.attempts() }} attempts remaining before lockout
          </div>
          <button
            class="btn btn-primary"
            (click)="verify()"
            [disabled]="code.length !== 6 || store.verifying()"
          >
            {{ store.verifying() ? 'Verifying...' : 'Verify' }}
          </button>
          @if (code.length !== 6 && !store.verifying()) {
            <p class="helper-text">Enter all 6 digits to enable verification</p>
          }
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
    .code-input:focus-visible { border-color: #3b82f6; outline: 2px solid #3b82f6; outline-offset: 2px; }
    .code-input:focus:not(:focus-visible) { border-color: #3b82f6; outline: none; }
    .input-error { border-color: #dc2626; }
    .field-error { color: #dc2626; font-size: 0.875rem; }
    .attempts-info { color: #9ca3af; font-size: 0.8rem; }
    .btn { padding: 0.75rem 1.5rem; border: none; border-radius: 0.5rem; cursor: pointer; font-size: 1rem; }
    .btn-primary { background: #3b82f6; color: white; }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
    .lockout { background: #fef3c7; border: 1px solid #fbbf24; border-radius: 0.5rem; padding: 1.5rem; margin-top: 1rem; }
    .lockout p { color: #92400e; font-weight: 500; }
    .helper-text { color: #9ca3af; font-size: 0.8rem; margin: 0; }
  `],
})
export class MfaLoginComponent implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  readonly store = inject(AuthStore);

  readonly MAX_ATTEMPTS = MAX_ATTEMPTS;

  code = '';
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
    if (this.code.length !== 6 || this.store.locked()) return;

    this.store.verifyLogin(this.code, {
      onSuccess: () => this.router.navigate(['/']),
    });
  }

  formatTime(seconds: number): string {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  private startTimer(): void {
    // Persist lockout end time for cross-reload survival
    const lockoutUntil = Date.now() + this.store.remainingSeconds() * 1000;
    sessionStorage.setItem('mfa_lockout_until', lockoutUntil.toString());

    this.timerInterval = setInterval(() => {
      this.store.tickLockout();
      if (!this.store.locked()) {
        if (this.timerInterval) clearInterval(this.timerInterval);
        sessionStorage.removeItem('mfa_lockout_until');
      }
    }, 1000);
  }

  private startLockoutIfNeeded(): void {
    const lockoutUntil = sessionStorage.getItem('mfa_lockout_until');
    if (lockoutUntil) {
      const timestamp = Number(lockoutUntil);
      this.store.restoreLockout(timestamp);
      if (this.store.locked()) {
        this.startTimer();
      } else {
        sessionStorage.removeItem('mfa_lockout_until');
      }
    }
  }
}
