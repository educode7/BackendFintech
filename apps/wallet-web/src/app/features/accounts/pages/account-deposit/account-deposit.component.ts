import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AccountService } from '@services/account.service';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { ErrorDisplayComponent } from '@shared/components/error-display/error-display.component';

@Component({
  selector: 'app-account-deposit',
  standalone: true,
  imports: [FormsModule, PageHeaderComponent, ErrorDisplayComponent],
  template: `
    <app-page-header title="Deposit Funds" />

    <div class="form-card">
      <div class="form-group">
        <label for="amount">Amount</label>
        <input id="amount" type="number" step="0.01" min="0.0001"
               [(ngModel)]="amount" placeholder="0.00" />
      </div>
      <div class="form-group">
        <label for="currency">Currency</label>
        <input id="currency" type="text" [(ngModel)]="currency" value="USD" />
      </div>

      @if (error()) {
        <app-error-display [detail]="error()!" />
      }

      <button class="btn btn-primary" (click)="deposit()" [disabled]="submitting()">
        {{ submitting() ? 'Processing...' : 'Deposit' }}
      </button>
    </div>
  `,
  styles: [`
    .form-card { background: white; border: 1px solid #e5e7eb; border-radius: 0.5rem; padding: 1.5rem; max-width: 400px; }
    .form-group { margin-bottom: 1rem; }
    .form-group label { display: block; margin-bottom: 0.25rem; font-weight: 500; color: #374151; }
    .form-group input { width: 100%; padding: 0.5rem; border: 1px solid #d1d5db; border-radius: 0.375rem; }
    .btn { padding: 0.5rem 1rem; border: none; border-radius: 0.375rem; cursor: pointer; font-weight: 500; }
    .btn-primary { background: #3b82f6; color: white; }
    .btn:disabled { opacity: 0.5; cursor: not-allowed; }
  `],
})
export class AccountDepositComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly accountService = inject(AccountService);

  amount = '';
  currency = 'USD';
  submitting = signal(false);
  error = signal<string | null>(null);

  deposit() {
    const accountId = this.route.snapshot.paramMap.get('accountId')!;
    this.submitting.set(true);
    this.accountService.deposit(accountId, { amount: this.amount, currency: this.currency }).subscribe({
      next: () => { this.router.navigate(['/accounts', accountId]); },
      error: (err) => { this.error.set(err.detail); this.submitting.set(false); },
    });
  }
}
