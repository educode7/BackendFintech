import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AccountStore } from '../../../application/stores/account.store';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { ErrorDisplayComponent } from '@shared/components/error-display/error-display.component';
import { AuthService } from '@core/infrastructure/auth.service';

@Component({
  selector: 'app-account-create',
  standalone: true,
  imports: [FormsModule, PageHeaderComponent, ErrorDisplayComponent],
  template: `
    <app-page-header title="New Account" subtitle="Open a new wallet account" />

    <div class="form-card">
      <!-- Account Information -->
      <h3 class="section-title">Account Information</h3>

      <div class="form-group">
        <label for="userId">User ID *</label>
        <input id="userId" type="text" [(ngModel)]="userId" placeholder="User ID" required />
      </div>

      <div class="form-grid">
        <div class="form-group">
          <label for="accountType">Account Type</label>
          <select id="accountType" [(ngModel)]="accountType">
            <option value="SAVINGS">Savings</option>
            <option value="CHECKING">Checking</option>
            <option value="DEPOSIT">Deposit</option>
          </select>
        </div>

        <div class="form-group">
          <label for="currency">Currency</label>
          <input id="currency" type="text" [(ngModel)]="currency" />
        </div>
      </div>

      <div class="form-grid">
        <div class="form-group">
          <label for="country">Country</label>
          <input id="country" type="text" [(ngModel)]="country" />
        </div>

        <div class="form-group">
          <label for="initialAmount">Initial Balance *</label>
          <input id="initialAmount" type="number" step="0.01" min="0.0001"
                 [(ngModel)]="initialAmount" placeholder="0.00" />
        </div>
      </div>

      <div class="form-group">
        <label for="initialCurrency">Initial Balance Currency</label>
        <input id="initialCurrency" type="text" [(ngModel)]="initialCurrency" />
      </div>

      <!-- Banking Details -->
      <h3 class="section-title">Banking Details</h3>

      <div class="form-grid">
        <div class="form-group">
          <label for="accountNumber">Account Number</label>
          <input id="accountNumber" type="text" [(ngModel)]="accountNumber" placeholder="Account number" maxlength="20" />
        </div>

        <div class="form-group">
          <label for="cci">CCI</label>
          <input id="cci" type="text" [(ngModel)]="cci" placeholder="CCI" maxlength="20" />
        </div>
      </div>

      <div class="form-grid">
        <div class="form-group">
          <label for="iban">IBAN</label>
          <input id="iban" type="text" [(ngModel)]="iban" placeholder="IBAN" maxlength="34" />
        </div>

        <div class="form-group">
          <label for="swiftBic">SWIFT/BIC</label>
          <input id="swiftBic" type="text" [(ngModel)]="swiftBic" placeholder="SWIFT/BIC" maxlength="11" />
        </div>
      </div>

      <div class="form-grid">
        <div class="form-group">
          <label for="bankCode">Bank Code</label>
          <input id="bankCode" type="text" [(ngModel)]="bankCode" placeholder="Bank code" maxlength="20" />
        </div>

        <div class="form-group">
          <label for="bankName">Bank Name</label>
          <input id="bankName" type="text" [(ngModel)]="bankName" placeholder="Bank name" maxlength="100" />
        </div>
      </div>

      <!-- Account Holder -->
      <h3 class="section-title">Account Holder</h3>

      <div class="form-group">
        <label for="holderName">Holder Name</label>
          <input id="holderName" type="text" [(ngModel)]="holderName" placeholder="Full name" maxlength="120" />
      </div>

      <div class="form-grid">
        <div class="form-group">
          <label for="holderDocumentType">Document Type</label>
          <select id="holderDocumentType" [(ngModel)]="holderDocumentType">
            <option value="">Select...</option>
            <option value="DNI">DNI</option>
            <option value="RUC">RUC</option>
            <option value="CE">CE</option>
            <option value="PASSPORT">Passport</option>
            <option value="RFC">RFC</option>
            <option value="CURP">CURP</option>
          </select>
        </div>

        <div class="form-group">
          <label for="holderDocumentNumber">Document Number</label>
          <input id="holderDocumentNumber" type="text" [(ngModel)]="holderDocumentNumber" placeholder="Document number" maxlength="20" />
        </div>
      </div>

      <div class="form-grid">
        <div class="form-group">
          <label for="holderEmail">Email</label>
          <input id="holderEmail" type="email" [(ngModel)]="holderEmail" placeholder="Email address" />
        </div>

        <div class="form-group">
          <label for="holderPhone">Phone</label>
          <input id="holderPhone" type="tel" [(ngModel)]="holderPhone" placeholder="Phone number" maxlength="20" />
        </div>
      </div>

      <!-- Limits -->
      <h3 class="section-title">Limits</h3>

      <div class="form-grid">
        <div class="form-group">
          <label for="dailyLimit">Daily Limit</label>
          <input id="dailyLimit" type="number" step="0.01" max="999999999999"
                 [(ngModel)]="dailyLimit" placeholder="0.00" />
        </div>

        <div class="form-group">
          <label for="monthlyLimit">Monthly Limit</label>
          <input id="monthlyLimit" type="number" step="0.01" max="999999999999"
                 [(ngModel)]="monthlyLimit" placeholder="0.00" />
        </div>
      </div>

      <div class="form-grid">
        <div class="form-group">
          <label for="singleTransactionLimit">Single Transaction Limit</label>
          <input id="singleTransactionLimit" type="number" step="0.01" max="999999999999"
                 [(ngModel)]="singleTransactionLimit" placeholder="0.00" />
        </div>

        <div class="form-group">
          <label for="overdraftLimit">Overdraft Limit</label>
          <input id="overdraftLimit" type="number" step="0.01" max="999999999999"
                 [(ngModel)]="overdraftLimit" placeholder="0.00" />
        </div>
      </div>

      @if (store.error()) {
        <app-error-display [detail]="store.error()!" />
      }

      <div class="form-actions">
        <button class="btn btn-secondary" type="button" (click)="cancel()">Cancel</button>
        <button class="btn btn-primary" (click)="submit()" [disabled]="store.loading() || !isValid()">
          {{ store.loading() ? 'Creating...' : 'Create Account' }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    .form-card { background: white; border: 1px solid #e5e7eb; border-radius: 0.5rem; padding: 1.5rem; max-width: 600px; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
    .section-title { font-size: 0.875rem; font-weight: 600; color: #374151; text-transform: uppercase; letter-spacing: 0.05em; margin: 1.5rem 0 0.75rem; border-bottom: 1px solid #e5e7eb; padding-bottom: 0.5rem; }
    .section-title:first-child { margin-top: 0; }
    .form-group { margin-bottom: 1rem; }
    .form-group label { display: block; margin-bottom: 0.25rem; font-weight: 500; color: #374151; }
    .form-group input, .form-group select {
      width: 100%; padding: 0.5rem; border: 1px solid #d1d5db; border-radius: 0.375rem;
    }
    .form-actions { display: flex; gap: 0.75rem; margin-top: 1.5rem; }
    .btn { padding: 0.5rem 1rem; border: none; border-radius: 0.375rem; cursor: pointer; font-weight: 500; }
    .btn-primary { background: #3b82f6; color: white; }
    .btn-primary:hover { background: #2563eb; }
    .btn-secondary { background: #e5e7eb; color: #374151; }
    .btn-secondary:hover { background: #d1d5db; }
    .btn:disabled { opacity: 0.5; cursor: not-allowed; }
  `],
})
export class AccountCreateComponent {
  readonly store = inject(AccountStore);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  // Account Information
  userId = this.authService.getUserInfo()?.sub ?? '';

  accountType = 'SAVINGS';
  currency = 'USD';
  country = 'PE';
  initialAmount: number | null = null;
  initialCurrency = 'USD';

  // Banking Details
  accountNumber = '';
  cci = '';
  iban = '';
  swiftBic = '';
  bankCode = '';
  bankName = '';

  // Account Holder
  holderName = '';
  holderDocumentType = '';
  holderDocumentNumber = '';
  holderEmail = '';
  holderPhone = '';

  // Limits
  dailyLimit: number | null = null;
  monthlyLimit: number | null = null;
  singleTransactionLimit: number | null = null;
  overdraftLimit: number | null = null;

  isValid(): boolean {
    return !!this.userId && this.initialAmount !== null && this.initialAmount > 0;
  }

  submit() {
    if (!this.isValid()) return;

    this.store.openAccount({
      userId: this.userId,
      initialBalance: {
        amount: String(this.initialAmount),
        currency: this.initialCurrency || this.currency,
      },
      accountType: this.accountType || undefined,
      currency: this.currency || undefined,
      country: this.country || undefined,
      accountNumber: this.accountNumber || undefined,
      cci: this.cci || undefined,
      iban: this.iban || undefined,
      swiftBic: this.swiftBic || undefined,
      bankCode: this.bankCode || undefined,
      bankName: this.bankName || undefined,
      holderName: this.holderName || undefined,
      holderDocumentType: this.holderDocumentType || undefined,
      holderDocumentNumber: this.holderDocumentNumber || undefined,
      holderEmail: this.holderEmail || undefined,
      holderPhone: this.holderPhone || undefined,
      dailyLimit: this.dailyLimit != null ? String(this.dailyLimit) : undefined,
      monthlyLimit: this.monthlyLimit != null ? String(this.monthlyLimit) : undefined,
      singleTransactionLimit: this.singleTransactionLimit != null ? String(this.singleTransactionLimit) : undefined,
      overdraftLimit: this.overdraftLimit != null ? String(this.overdraftLimit) : undefined,
    });

    setTimeout(() => {
      this.router.navigate(['/accounts']);
    }, 1000);
  }

  cancel() {
    this.router.navigate(['/accounts']);
  }
}
