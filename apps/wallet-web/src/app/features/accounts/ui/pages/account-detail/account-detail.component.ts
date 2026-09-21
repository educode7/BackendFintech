import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AccountStore } from '../../../application/stores/account.store';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { ErrorDisplayComponent } from '@shared/components/error-display/error-display.component';
import { CurrencyPipe } from '@shared/pipes/currency.pipe';

@Component({
  selector: 'app-account-detail',
  standalone: true,
  imports: [PageHeaderComponent, LoadingSpinnerComponent, ErrorDisplayComponent, CurrencyPipe],
  template: `
    <app-page-header title="Account Detail" [subtitle]="'ID: ' + store.selectedAccount()?.accountId" />

    @if (store.loading()) {
      <app-loading-spinner />
    } @else if (store.error()) {
      <app-error-display [detail]="store.error()!" />
    } @else if (store.selectedAccount(); as a) {
      <!-- Balance Card -->
      <div class="balance-card">
        <div class="balance-main">
          <span class="balance-label">Balance</span>
          <span class="balance-amount">{{ a.balanceAmount | currency:a.balanceCurrency }}</span>
        </div>
        <div class="balance-details">
          @if (a.availableAmount) {
            <div class="balance-item">
              <span class="item-label">Available</span>
              <span class="item-value">{{ a.availableAmount | currency:a.availableAmountCurrency }}</span>
            </div>
          }
          @if (a.holdAmount && a.holdAmount > 0) {
            <div class="balance-item">
              <span class="item-label">On Hold</span>
              <span class="item-value hold">{{ a.holdAmount | currency:a.holdAmountCurrency }}</span>
            </div>
          }
          @if (a.overdraftLimit && a.overdraftLimit > 0) {
            <div class="balance-item">
              <span class="item-label">Overdraft Limit</span>
              <span class="item-value">{{ a.overdraftLimit | currency:a.overdraftLimitCurrency }}</span>
            </div>
          }
        </div>
      </div>

      <!-- Identification Section -->
      <div class="detail-section">
        <h3 class="section-title">Identification</h3>
        <div class="detail-card">
          <div class="detail-row">
            <span class="label">Account Number</span>
            <span class="value mono">{{ a.accountNumber || '—' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Account Type</span>
            <span class="value badge">{{ a.accountType || '—' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">CCI</span>
            <span class="value mono">{{ a.cci || '—' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">IBAN</span>
            <span class="value mono">{{ a.iban || '—' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">SWIFT/BIC</span>
            <span class="value mono">{{ a.swiftBic || '—' }}</span>
          </div>
        </div>
      </div>

      <!-- Holder Info Section -->
      <div class="detail-section">
        <h3 class="section-title">Account Holder</h3>
        <div class="detail-card">
          <div class="detail-row">
            <span class="label">Name</span>
            <span class="value">{{ a.holderName || '—' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Document Type</span>
            <span class="value">{{ a.holderDocumentType || '—' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Document Number</span>
            <span class="value mono">{{ a.holderDocumentNumber || '—' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Email</span>
            <span class="value">{{ a.holderEmail || '—' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Phone</span>
            <span class="value">{{ a.holderPhone || '—' }}</span>
          </div>
        </div>
      </div>

      <!-- Banking Section -->
      <div class="detail-section">
        <h3 class="section-title">Banking</h3>
        <div class="detail-card">
          <div class="detail-row">
            <span class="label">Bank Code</span>
            <span class="value mono">{{ a.bankCode || '—' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Bank Name</span>
            <span class="value">{{ a.bankName || '—' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Currency</span>
            <span class="value">{{ a.currency || '—' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Country</span>
            <span class="value">{{ a.country || '—' }}</span>
          </div>
        </div>
      </div>

      <!-- Limits Section -->
      <div class="detail-section">
        <h3 class="section-title">Limits</h3>
        <div class="detail-card">
          <div class="detail-row">
            <span class="label">Daily Limit</span>
            <span class="value">{{ a.dailyLimit ? (a.dailyLimit | currency:a.dailyLimitCurrency) : '—' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Monthly Limit</span>
            <span class="value">{{ a.monthlyLimit ? (a.monthlyLimit | currency:a.monthlyLimitCurrency) : '—' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Single Transaction Limit</span>
            <span class="value">{{ a.singleTransactionLimit ? (a.singleTransactionLimit | currency:a.singleTransactionLimitCurrency) : '—' }}</span>
          </div>
        </div>
      </div>

      <!-- Status & Metadata -->
      <div class="detail-section">
        <h3 class="section-title">Status & Metadata</h3>
        <div class="detail-card">
          <div class="detail-row">
            <span class="label">Status</span>
            <span class="value status" [class.open]="a.status === 'OPEN'" [class.closed]="a.status === 'CLOSED'">{{ a.status }}</span>
          </div>
          <div class="detail-row">
            <span class="label">User ID</span>
            <span class="value mono">{{ a.userId }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Version</span>
            <span class="value">{{ a.version }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Activated At</span>
            <span class="value">{{ a.activatedAt || '—' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Closed At</span>
            <span class="value">{{ a.closedAt || '—' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Last Updated</span>
            <span class="value">{{ a.lastUpdated }}</span>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .balance-card {
      background: linear-gradient(135deg, #1e40af, #3b82f6);
      color: white;
      border-radius: 0.75rem;
      padding: 1.5rem;
      margin-bottom: 1.5rem;
    }
    .balance-main { text-align: center; margin-bottom: 1rem; }
    .balance-label { display: block; font-size: 0.875rem; opacity: 0.8; margin-bottom: 0.25rem; }
    .balance-amount { font-size: 2rem; font-weight: 700; }
    .balance-details { display: flex; justify-content: space-around; padding-top: 1rem; border-top: 1px solid rgba(255,255,255,0.2); }
    .balance-item { text-align: center; }
    .item-label { display: block; font-size: 0.75rem; opacity: 0.8; }
    .item-value { font-size: 1rem; font-weight: 600; }
    .item-value.hold { color: #fbbf24; }
    .detail-section { margin-bottom: 1.5rem; }
    .section-title { font-size: 0.875rem; font-weight: 600; color: #374151; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 0.75rem; }
    .detail-card { background: white; border: 1px solid #e5e7eb; border-radius: 0.5rem; padding: 1rem; }
    .detail-row { display: flex; justify-content: space-between; padding: 0.5rem 0; border-bottom: 1px solid #f3f4f6; }
    .detail-row:last-child { border-bottom: none; }
    .label { color: #6b7280; font-weight: 500; font-size: 0.875rem; }
    .value { color: #111827; font-size: 0.875rem; }
    .mono { font-family: monospace; }
    .badge { background: #f3f4f6; padding: 0.125rem 0.5rem; border-radius: 0.25rem; font-size: 0.75rem; }
    .status { font-weight: 600; }
    .status.open { color: #059669; }
    .status.closed { color: #dc2626; }
  `],
})
export class AccountDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  readonly store = inject(AccountStore);

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('accountId')!;
    this.store.loadAccount(id);
  }
}
