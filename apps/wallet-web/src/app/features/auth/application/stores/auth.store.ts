import { Injectable, signal, inject } from '@angular/core';
import { AuthAdapter } from '../../infrastructure/auth.adapter';
import type { MfaSetupResponse } from '../../domain/mfa.model';

/**
 * Auth store — manages MFA state with signals.
 */
@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly adapter: AuthAdapter;

  // MFA Setup state
  private readonly _setupData = signal<MfaSetupResponse | null>(null);
  private readonly _setupLoading = signal(false);
  private readonly _setupError = signal<string | null>(null);
  private readonly _setupComplete = signal(false);

  // MFA Verify state
  private readonly _verifying = signal(false);
  private readonly _verifyError = signal<string | null>(null);

  // MFA Disable state
  private readonly _disableLoading = signal(false);
  private readonly _disableError = signal<string | null>(null);
  private readonly _disableSuccess = signal(false);

  // MFA Login lockout state
  private readonly _locked = signal(false);
  private readonly _remainingSeconds = signal(0);
  private readonly _attempts = signal(0);

  // Public readonly signals
  readonly setupData = this._setupData.asReadonly();
  readonly setupLoading = this._setupLoading.asReadonly();
  readonly setupError = this._setupError.asReadonly();
  readonly setupComplete = this._setupComplete.asReadonly();
  readonly verifying = this._verifying.asReadonly();
  readonly verifyError = this._verifyError.asReadonly();
  readonly disableLoading = this._disableLoading.asReadonly();
  readonly disableError = this._disableError.asReadonly();
  readonly disableSuccess = this._disableSuccess.asReadonly();
  readonly locked = this._locked.asReadonly();
  readonly remainingSeconds = this._remainingSeconds.asReadonly();
  readonly attempts = this._attempts.asReadonly();

  constructor(adapter: AuthAdapter) {
    this.adapter = adapter;
  }

  /** Initiate MFA setup — calls POST /auth/mfa/setup */
  initSetup(): void {
    this._setupLoading.set(true);
    this._setupError.set(null);
    this._setupData.set(null);
    this._setupComplete.set(false);

    this.adapter.setup().subscribe({
      next: (data) => {
        this._setupData.set(data);
        this._setupLoading.set(false);
      },
      error: (err) => {
        this._setupError.set(err.detail || 'Failed to initiate MFA setup');
        this._setupLoading.set(false);
      },
    });
  }

  /** Verify code during setup — calls POST /auth/mfa/verify */
  verifySetup(code: string): void {
    this._verifying.set(true);
    this._verifyError.set(null);

    this.adapter.verify(code).subscribe({
      next: (res) => {
        this._verifying.set(false);
        if (res.verified) {
          this._setupComplete.set(true);
        } else {
          this._verifyError.set('Invalid code. Please try again.');
        }
      },
      error: (err) => {
        this._verifying.set(false);
        this._verifyError.set(
          err.detail || 'Verification failed. Try again.',
        );
      },
    });
  }

  /** Verify code during login — calls POST /auth/mfa/verify, manages lockout state */
  verifyLogin(
    code: string,
    options?: { onSuccess?: () => void },
  ): void {
    this._verifying.set(true);
    this._verifyError.set(null);

    this.adapter.verify(code).subscribe({
      next: (res) => {
        this._verifying.set(false);
        if (res.verified) {
          options?.onSuccess?.();
        } else {
          this.handleFailure('Invalid code. Please try again.');
        }
      },
      error: (err) => {
        this._verifying.set(false);
        this.handleFailure(err.detail || 'Verification failed.');
      },
    });
  }

  /** Disable MFA — calls POST /auth/mfa/disable */
  disableMfa(code: string): void {
    this._disableLoading.set(true);
    this._disableError.set(null);
    this._disableSuccess.set(false);

    this.adapter.disable(code).subscribe({
      next: () => {
        this._disableLoading.set(false);
        this._disableSuccess.set(true);
      },
      error: (err) => {
        this._disableLoading.set(false);
        this._disableError.set(
          err.detail || 'Failed to disable MFA. Check your code.',
        );
      },
    });
  }

  clearSetupError(): void {
    this._setupError.set(null);
  }

  clearVerifyError(): void {
    this._verifyError.set(null);
  }

  clearDisableError(): void {
    this._disableError.set(null);
  }

  /** Increment attempts and manage lockout state */
  handleFailure(_message: string): void {
    const newAttempts = this._attempts() + 1;
    this._attempts.set(newAttempts);
    this._verifyError.set(_message);

    if (newAttempts >= 5) {
      this._locked.set(true);
      this._remainingSeconds.set(300);
    }
  }

  /** Decrement remaining lockout seconds (called by component timer) */
  tickLockout(): void {
    const remaining = this._remainingSeconds() - 1;
    if (remaining <= 0) {
      this._locked.set(false);
      this._attempts.set(0);
      this._remainingSeconds.set(0);
    } else {
      this._remainingSeconds.set(remaining);
    }
  }

  /** Restore lockout state from sessionStorage */
  restoreLockout(untilTimestamp: number): void {
    const remaining = Math.ceil((untilTimestamp - Date.now()) / 1000);
    if (remaining > 0) {
      this._locked.set(true);
      this._remainingSeconds.set(remaining);
      this._attempts.set(5);
    }
  }

  reset(): void {
    this._setupData.set(null);
    this._setupLoading.set(false);
    this._setupError.set(null);
    this._setupComplete.set(false);
    this._verifying.set(false);
    this._verifyError.set(null);
    this._disableLoading.set(false);
    this._disableError.set(null);
    this._disableSuccess.set(false);
    this._locked.set(false);
    this._remainingSeconds.set(0);
    this._attempts.set(0);
  }
}
