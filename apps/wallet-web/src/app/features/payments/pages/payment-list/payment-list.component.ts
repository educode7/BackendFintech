import { Component, inject, signal, computed } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { PaymentService } from '@services/payment.service';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { ErrorDisplayComponent } from '@shared/components/error-display/error-display.component';
import { CurrencyPipe } from '@shared/pipes/currency.pipe';
import { RelativeTimePipe } from '@shared/pipes/relative-time.pipe';

@Component({
  selector: 'app-payment-list',
  standalone: true,
  imports: [PageHeaderComponent, LoadingSpinnerComponent, ErrorDisplayComponent, CurrencyPipe, RelativeTimePipe],
  template: `
    <app-page-header title="Payments" subtitle="View and create payments" />

    <div class="actions">
      <button class="btn btn-primary" (click)="openCreate()">New Payment</button>
    </div>

    @if (loading()) {
      <app-loading-spinner message="Loading payments..." />
    } @else if (error()) {
      <app-error-display [detail]="error()!" title="Failed to load payments" />
    } @else {
      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>User</th>
              <th>Amount</th>
              <th>Status</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            @for (payment of payments(); track payment.id) {
              <tr class="clickable" (click)="viewDetail(payment.id)">
                <td class="mono">{{ payment.id }}</td>
                <td>{{ payment.userId }}</td>
                <td>{{ payment.amount.amount | currency:payment.amount.currency }}</td>
                <td>
                  <span class="badge" [class]="payment.status.toLowerCase()">
                    {{ payment.status }}
                  </span>
                </td>
                <td>{{ payment.createdAt | relativeTime }}</td>
              </tr>
            } @empty {
              <tr><td colspan="5" class="empty">No payments found</td></tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
  styles: [`
    .actions { margin-bottom: 1.5rem; }
    .btn { padding: 0.5rem 1rem; border: none; border-radius: 0.375rem; cursor: pointer; font-weight: 500; }
    .btn-primary { background: #3b82f6; color: white; }
    .btn-primary:hover { background: #2563eb; }
    .table-container { overflow-x: auto; }
    .data-table { width: 100%; border-collapse: collapse; }
    .data-table th, .data-table td { padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid #e5e7eb; }
    .data-table th { background: #f9fafb; font-weight: 600; color: #374151; }
    .clickable { cursor: pointer; }
    .clickable:hover { background: #f3f4f6; }
    .mono { font-family: monospace; font-size: 0.875rem; }
    .empty { text-align: center; color: #999; padding: 2rem; }
    .badge { padding: 0.25rem 0.5rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 500; }
    .badge.completed { background: #dcfce7; color: #166534; }
    .badge.pending { background: #fef9c3; color: #854d0e; }
    .badge.processing { background: #dbeafe; color: #1e40af; }
    .badge.failed { background: #fee2e2; color: #991b1b; }
  `],
})
export class PaymentListComponent {
  private readonly paymentService = inject(PaymentService);
  private readonly router = inject(Router);

  loading = signal(true);
  error = signal<string | null>(null);
  payments = signal<any[]>([]);

  constructor() {
    this.loadPayments();
  }

  loadPayments() {
    this.loading.set(true);
    this.paymentService.list().subscribe({
      next: (res) => {
        this.payments.set(res.data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.detail || 'Failed to load payments');
        this.loading.set(false);
      },
    });
  }

  openCreate() {
    this.router.navigate(['/payments/new']);
  }

  viewDetail(id: string) {
    this.router.navigate(['/payments', id]);
  }
}
