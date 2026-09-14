import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AccountService } from '@services/account.service';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { ErrorDisplayComponent } from '@shared/components/error-display/error-display.component';
import { CurrencyPipe } from '@shared/pipes/currency.pipe';
import type { Account } from '@models/account.model';

@Component({
  selector: 'app-account-detail',
  standalone: true,
  imports: [PageHeaderComponent, LoadingSpinnerComponent, ErrorDisplayComponent, CurrencyPipe],
  template: `
    <app-page-header title="Account Detail" [subtitle]="'ID: ' + account()?.accountId" />

    @if (loading()) {
      <app-loading-spinner />
    } @else if (error()) {
      <app-error-display [detail]="error()!" />
    } @else if (account(); as a) {
      <div class="detail-card">
        <div class="detail-row">
          <span class="label">Balance</span>
          <span class="balance">{{ a.balance.amount | currency:a.balance.currency }}</span>
        </div>
        <div class="detail-row">
          <span class="label">User</span>
          <span class="value mono">{{ a.userId }}</span>
        </div>
        <div class="detail-row">
          <span class="label">Version</span>
          <span class="value">{{ a.version }}</span>
        </div>
      </div>
    }
  `,
  styles: [`
    .detail-card { background: white; border: 1px solid #e5e7eb; border-radius: 0.5rem; padding: 1.5rem; }
    .detail-row { display: flex; justify-content: space-between; padding: 0.75rem 0; border-bottom: 1px solid #f3f4f6; }
    .detail-row:last-child { border-bottom: none; }
    .label { color: #6b7280; font-weight: 500; }
    .value { color: #111827; }
    .balance { font-size: 1.5rem; font-weight: 700; color: #059669; }
    .mono { font-family: monospace; }
  `],
})
export class AccountDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly accountService = inject(AccountService);

  loading = signal(true);
  error = signal<string | null>(null);
  account = signal<Account | null>(null);

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('accountId')!;
    this.accountService.getById(id).subscribe({
      next: (a) => { this.account.set(a); this.loading.set(false); },
      error: (err) => { this.error.set(err.detail); this.loading.set(false); },
    });
  }
}
