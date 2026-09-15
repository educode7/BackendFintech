import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AccountStore } from '../../../application/stores/account.store';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { ErrorDisplayComponent } from '@shared/components/error-display/error-display.component';
import { CurrencyPipe } from '@shared/pipes/currency.pipe';

@Component({
  selector: 'app-account-list',
  standalone: true,
  imports: [PageHeaderComponent, LoadingSpinnerComponent, ErrorDisplayComponent, CurrencyPipe],
  template: `
    <app-page-header title="Accounts" subtitle="Manage wallet accounts" />

    <div class="actions">
      <button class="btn btn-primary" (click)="openCreate()">New Account</button>
    </div>

    @if (store.loading()) {
      <app-loading-spinner message="Loading accounts..." />
    } @else if (store.error()) {
      <app-error-display [detail]="store.error()!" title="Failed to load accounts" />
    } @else {
      <div class="account-grid">
        @for (account of store.accounts(); track account.accountId) {
          <div class="account-card" (click)="viewDetail(account.accountId)">
            <div class="account-header">
              <span class="mono">{{ account.accountId }}</span>
              <span class="balance">{{ account.balance.amount | currency:account.balance.currency }}</span>
            </div>
            <div class="account-footer">
              <span>v{{ account.version }}</span>
            </div>
          </div>
        } @empty {
          <p class="empty">No accounts found</p>
        }
      </div>
    }
  `,
  styles: [`
    .actions { margin-bottom: 1.5rem; }
    .btn { padding: 0.5rem 1rem; border: none; border-radius: 0.375rem; cursor: pointer; font-weight: 500; }
    .btn-primary { background: #3b82f6; color: white; }
    .account-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 1rem; }
    .account-card {
      background: white; border: 1px solid #e5e7eb; border-radius: 0.5rem;
      padding: 1.25rem; cursor: pointer; transition: box-shadow 0.2s;
    }
    .account-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
    .account-header { display: flex; justify-content: space-between; align-items: center; }
    .mono { font-family: monospace; font-size: 0.875rem; color: #6b7280; }
    .balance { font-size: 1.25rem; font-weight: 600; color: #111827; }
    .account-footer { margin-top: 0.75rem; color: #9ca3af; font-size: 0.75rem; }
    .empty { text-align: center; color: #999; grid-column: 1/-1; padding: 2rem; }
  `],
})
export class AccountListComponent implements OnInit {
  private readonly router = inject(Router);
  readonly store = inject(AccountStore);

  ngOnInit() {
    this.store.loadAccounts();
  }

  openCreate() {
    this.router.navigate(['/accounts/new']);
  }

  viewDetail(id: string) {
    this.router.navigate(['/accounts', id]);
  }
}
