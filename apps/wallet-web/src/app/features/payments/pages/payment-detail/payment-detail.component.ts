import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PaymentService } from '@services/payment.service';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { ErrorDisplayComponent } from '@shared/components/error-display/error-display.component';
import { CurrencyPipe } from '@shared/pipes/currency.pipe';
import { RelativeTimePipe } from '@shared/pipes/relative-time.pipe';
import type { Payment } from '@models/payment.model';

@Component({
  selector: 'app-payment-detail',
  standalone: true,
  imports: [PageHeaderComponent, LoadingSpinnerComponent, ErrorDisplayComponent, CurrencyPipe, RelativeTimePipe],
  template: `
    <app-page-header title="Payment Detail" [subtitle]="'ID: ' + payment()?.id" />

    @if (loading()) {
      <app-loading-spinner />
    } @else if (error()) {
      <app-error-display [detail]="error()!" />
    } @else if (payment(); as p) {
      <div class="detail-card">
        <div class="detail-row">
          <span class="label">Status</span>
          <span class="badge" [class]="p.status.toLowerCase()">{{ p.status }}</span>
        </div>
        <div class="detail-row">
          <span class="label">Amount</span>
          <span class="value">{{ p.amount.amount | currency:p.amount.currency }}</span>
        </div>
        <div class="detail-row">
          <span class="label">User</span>
          <span class="value mono">{{ p.userId }}</span>
        </div>
        <div class="detail-row">
          <span class="label">Created</span>
          <span class="value">{{ p.createdAt | relativeTime }}</span>
        </div>
        <div class="detail-row">
          <span class="label">Idempotency Key</span>
          <span class="value mono">{{ p.idempotencyKey }}</span>
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
    .mono { font-family: monospace; }
    .badge { padding: 0.25rem 0.5rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 500; }
    .badge.completed { background: #dcfce7; color: #166534; }
    .badge.pending { background: #fef9c3; color: #854d0e; }
    .badge.processing { background: #dbeafe; color: #1e40af; }
    .badge.failed { background: #fee2e2; color: #991b1b; }
  `],
})
export class PaymentDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly paymentService = inject(PaymentService);

  loading = signal(true);
  error = signal<string | null>(null);
  payment = signal<Payment | null>(null);

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.paymentService.getById(id).subscribe({
      next: (p) => { this.payment.set(p); this.loading.set(false); },
      error: (err) => { this.error.set(err.detail); this.loading.set(false); },
    });
  }
}
