import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthStore } from '../../../application/stores/auth.store';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-mfa-setup',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="mfa-container">
      <h2>Set Up Two-Factor Authentication</h2>

      @if (store.setupLoading()) {
        <div class="loading">Setting up MFA...</div>
      } @else if (store.setupError()) {
        <div class="error" role="alert" aria-live="polite">{{ store.setupError() }}</div>
        <button class="btn" (click)="initSetup()">Retry</button>
      } @else if (store.setupData() && !store.setupComplete()) {
        <p class="instruction">
          Scan this QR code with your authenticator app (Google Authenticator, Authy, etc.):
        </p>
        <div class="qr-wrapper">
          <img [src]="store.setupData()!.qr_code" alt="MFA QR Code" class="qr-image" />
        </div>
        <div class="secret-box">
          <span class="label">Manual entry key:</span>
          <code>{{ store.setupData()!.secret }}</code>
        </div>

        <div class="verify-section">
          <label>Enter the 6-digit code from your app to verify:</label>
          <input
            type="text"
            [(ngModel)]="verifyCode"
            maxlength="6"
            pattern="[0-9]{6}"
            placeholder="000000"
            class="code-input"
            [class.input-error]="!!store.verifyError()"
            [attr.aria-invalid]="!!store.verifyError()"
            [attr.aria-describedby]="store.verifyError() ? 'setup-error-msg' : null"
            (input)="store.clearVerifyError()"
          />
          @if (store.verifyError()) {
            <div id="setup-error-msg" class="field-error" role="alert" aria-live="polite">{{ store.verifyError() }}</div>
          }
          <button class="btn btn-primary" (click)="verifySetup()" [disabled]="verifyCode.length !== 6 || store.verifying()">
            {{ store.verifying() ? 'Verifying...' : 'Verify & Activate' }}
          </button>
          @if (verifyCode.length !== 6 && !store.verifying()) {
            <p class="helper-text">Enter all 6 digits to enable verification</p>
          } @else if (store.verifying()) {
            <p class="helper-text">Please wait while we verify your code</p>
          }
        </div>
      } @else if (store.setupComplete()) {
        <div class="success">
          <h3>✅ MFA Enabled Successfully</h3>
          <p>Save these recovery codes. They will not be shown again:</p>
          <div class="recovery-codes">
            @for (code of store.setupData()!.recovery_codes; track code) {
              <code class="recovery-code">{{ code }}</code>
            }
          </div>
          <button class="btn btn-secondary" (click)="copyRecoveryCodes()">
            {{ copied ? '✓ Copied' : 'Copy All Codes' }}
          </button>
          <p class="warning">
            ⚠️ Each recovery code can only be used once. Store them securely.
          </p>
          <button class="btn btn-primary" (click)="goToDashboard()">Continue to Dashboard</button>
        </div>
      }
    </div>
  `,
  styles: [`
    .mfa-container { max-width: 480px; margin: 2rem auto; padding: 1.5rem; }
    h2 { margin-bottom: 1rem; }
    .loading, .error { text-align: center; padding: 2rem; font-size: 1.1rem; }
    .error { color: #dc2626; }
    .instruction { color: #4b5563; margin-bottom: 1rem; }
    .qr-wrapper { text-align: center; margin: 1.5rem 0; }
    .qr-image { max-width: 200px; border: 1px solid #e5e7eb; border-radius: 0.5rem; }
    .secret-box { background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 0.5rem; padding: 0.75rem 1rem; margin-bottom: 1.5rem; display: flex; align-items: center; gap: 0.5rem; }
    .secret-box .label { color: #6b7280; font-size: 0.875rem; }
    .secret-box code { font-weight: 600; letter-spacing: 0.1em; }
    .verify-section { display: flex; flex-direction: column; gap: 0.75rem; }
    .verify-section label { color: #374151; font-weight: 500; }
    .code-input { font-size: 1.5rem; letter-spacing: 0.3em; text-align: center; padding: 0.75rem; border: 2px solid #d1d5db; border-radius: 0.5rem; width: 100%; font-family: monospace; }
    .code-input:focus-visible { border-color: #3b82f6; outline: 2px solid #3b82f6; outline-offset: 2px; }
    .code-input:focus:not(:focus-visible) { border-color: #3b82f6; outline: none; }
    .input-error { border-color: #dc2626; }
    .field-error { color: #dc2626; font-size: 0.875rem; }
    .btn { padding: 0.75rem 1.5rem; border: none; border-radius: 0.5rem; cursor: pointer; font-size: 1rem; }
    .btn-primary { background: #3b82f6; color: white; }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-secondary { background: #e5e7eb; color: #374151; margin-bottom: 1rem; }
    .success { text-align: center; }
    .recovery-codes { display: grid; grid-template-columns: repeat(2, 1fr); gap: 0.5rem; margin: 1rem 0; }
    .recovery-code { background: #f3f4f6; padding: 0.5rem; border-radius: 0.25rem; font-family: monospace; font-size: 0.9rem; text-align: center; }
    .warning { color: #d97706; font-size: 0.875rem; margin: 1rem 0; }
    .helper-text { color: #6b7280; font-size: 0.8rem; margin: 0; }
  `],
})
export class MfaSetupComponent implements OnInit {
  private readonly router = inject(Router);
  readonly store = inject(AuthStore);

  verifyCode = '';
  copied = false;

  ngOnInit(): void {
    this.store.initSetup();
  }

  initSetup(): void {
    this.store.initSetup();
  }

  verifySetup(): void {
    if (this.verifyCode.length !== 6) return;
    this.store.verifySetup(this.verifyCode);
  }

  copyRecoveryCodes(): void {
    const data = this.store.setupData();
    if (!data?.recovery_codes) return;
    const text = data.recovery_codes.join('\n');
    navigator.clipboard.writeText(text).then(() => {
      this.copied = true;
      setTimeout(() => (this.copied = false), 2000);
    });
  }

  goToDashboard(): void {
    this.router.navigate(['/']);
  }
}
