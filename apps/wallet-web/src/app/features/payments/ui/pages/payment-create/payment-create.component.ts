import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PaymentStore } from '../../../application/stores/payment.store';
import { AccountStore } from '../../../../accounts/application/stores/account.store';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { ErrorDisplayComponent } from '@shared/components/error-display/error-display.component';
import type { Account } from '../../../../accounts/domain/account.model';

@Component({
  selector: 'app-payment-create',
  standalone: true,
  imports: [FormsModule, PageHeaderComponent, ErrorDisplayComponent],
  template: `
    <app-page-header title="New Payment" subtitle="Send a payment from an account" />

    <div class="form-card">
      <!-- Origin Section -->
      <h3 class="section-title">Origin</h3>

      <div class="form-group">
        <label for="account">From Account</label>
        <select id="account" [(ngModel)]="selectedAccountId" (ngModelChange)="onAccountChange($event)">
          <option value="" disabled>Select an account</option>
          @for (account of accountStore.accounts(); track account.accountId) {
            <option [value]="account.accountId">
              {{ account.accountNumber || account.accountId }} — {{ account.balanceAmount }} {{ account.balanceCurrency }}
            </option>
          }
        </select>
      </div>

      <div class="form-group">
        <label for="userId">User ID</label>
        <input id="userId" type="text" [(ngModel)]="userId" readonly placeholder="Auto-filled from account" />
      </div>

      <!-- Beneficiary Section -->
      <h3 class="section-title">Beneficiary</h3>

      <div class="form-group">
        <label for="beneficiaryName">Beneficiary Name</label>
        <input id="beneficiaryName" type="text" [(ngModel)]="beneficiaryName" placeholder="Full name" />
      </div>

      <div class="form-group">
        <label for="beneficiaryDocumentType">Document Type</label>
        <select id="beneficiaryDocumentType" [(ngModel)]="beneficiaryDocumentType">
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
        <label for="beneficiaryDocumentNumber">Document Number</label>
        <input id="beneficiaryDocumentNumber" type="text" [(ngModel)]="beneficiaryDocumentNumber" placeholder="Document number" />
      </div>

      <div class="form-group">
        <label for="beneficiaryAccountNumber">Account Number</label>
        <input id="beneficiaryAccountNumber" type="text" [(ngModel)]="beneficiaryAccountNumber" placeholder="Account number" />
      </div>

      <div class="form-group">
        <label for="beneficiaryBankCode">Bank Code</label>
        <input id="beneficiaryBankCode" type="text" [(ngModel)]="beneficiaryBankCode" placeholder="Bank code" />
      </div>

      <div class="form-group">
        <label for="beneficiaryBankName">Bank Name</label>
        <input id="beneficiaryBankName" type="text" [(ngModel)]="beneficiaryBankName" placeholder="Bank name" />
      </div>

      <!-- Payment Details Section -->
      <h3 class="section-title">Payment Details</h3>

      <div class="form-group">
        <label for="amount">Amount</label>
        <input id="amount" type="number" step="0.01" min="0.01"
               [(ngModel)]="amount" placeholder="0.00" />
      </div>

      <div class="form-group">
        <label for="currency">Currency</label>
        <input id="currency" type="text" [(ngModel)]="currency" value="USD" />
      </div>

      <div class="form-group">
        <label for="paymentType">Payment Type</label>
        <select id="paymentType" [(ngModel)]="paymentType">
          <option value="PAYMENT">Payment</option>
          <option value="TRANSFER">Transfer</option>
          <option value="REFUND">Refund</option>
        </select>
      </div>

      <div class="form-group">
        <label for="reference">Reference</label>
        <input id="reference" type="text" [(ngModel)]="reference" placeholder="Payment description" />
      </div>

      <div class="form-group">
        <label for="channel">Channel</label>
        <select id="channel" [(ngModel)]="channel">
          <option value="WEB">Web</option>
          <option value="MOBILE">Mobile</option>
          <option value="API">API</option>
          <option value="BATCH">Batch</option>
        </select>
      </div>

      @if (paymentStore.error()) {
        <app-error-display [detail]="paymentStore.error()!" />
      }

      <button class="btn btn-primary" (click)="submit()" [disabled]="paymentStore.loading() || !isValid()">
        {{ paymentStore.loading() ? 'Processing...' : 'Send Payment' }}
      </button>
    </div>
  `,
  styles: [`
    .form-card { background: white; border: 1px solid #e5e7eb; border-radius: 0.5rem; padding: 1.5rem; max-width: 500px; }
    .section-title { font-size: 0.875rem; font-weight: 600; color: #374151; text-transform: uppercase; letter-spacing: 0.05em; margin: 1.5rem 0 0.75rem; padding-bottom: 0.5rem; border-bottom: 1px solid #e5e7eb; }
    .section-title:first-child { margin-top: 0; }
    .form-group { margin-bottom: 1rem; }
    .form-group label { display: block; margin-bottom: 0.25rem; font-weight: 500; color: #374151; }
    .form-group input, .form-group select {
      width: 100%; padding: 0.5rem; border: 1px solid #d1d5db; border-radius: 0.375rem;
    }
    .form-group input[readonly] { background: #f9fafb; color: #6b7280; }
    .btn { padding: 0.5rem 1rem; border: none; border-radius: 0.375rem; cursor: pointer; font-weight: 500; }
    .btn-primary { background: #3b82f6; color: white; }
    .btn-primary:hover { background: #2563eb; }
    .btn:disabled { opacity: 0.5; cursor: not-allowed; }
  `],
})
export class PaymentCreateComponent implements OnInit {
  readonly paymentStore = inject(PaymentStore);
  readonly accountStore = inject(AccountStore);
  private readonly router = inject(Router);

  selectedAccountId = '';
  userId = '';
  amount = '';
  currency = 'USD';

  // Beneficiary fields
  beneficiaryName = '';
  beneficiaryDocumentType = '';
  beneficiaryDocumentNumber = '';
  beneficiaryAccountNumber = '';
  beneficiaryBankCode = '';
  beneficiaryBankName = '';

  // Payment details
  paymentType = 'PAYMENT';
  reference = '';
  channel = 'WEB';

  ngOnInit() {
    this.accountStore.loadAccounts(0, 100);
  }

  onAccountChange(accountId: string) {
    const account = this.accountStore.accounts().find(a => a.accountId === accountId);
    this.userId = account?.userId ?? '';
  }

  isValid(): boolean {
    return !!this.selectedAccountId && !!this.userId && Number(this.amount) > 0 && !!this.currency;
  }

  submit() {
    if (!this.isValid()) return;

    this.paymentStore.processPayment({
      accountId: this.selectedAccountId,
      userId: this.userId,
      amount: { amount: this.amount, currency: this.currency },
      paymentType: this.paymentType || undefined,
      beneficiaryName: this.beneficiaryName || undefined,
      beneficiaryDocumentType: this.beneficiaryDocumentType || undefined,
      beneficiaryDocumentNumber: this.beneficiaryDocumentNumber || undefined,
      beneficiaryAccountNumber: this.beneficiaryAccountNumber || undefined,
      beneficiaryBankCode: this.beneficiaryBankCode || undefined,
      beneficiaryBankName: this.beneficiaryBankName || undefined,
      reference: this.reference || undefined,
      channel: this.channel || undefined,
    });

    // Navigate back to payments list after a short delay to let the request complete
    setTimeout(() => {
      this.router.navigate(['/payments']);
    }, 1000);
  }
}
