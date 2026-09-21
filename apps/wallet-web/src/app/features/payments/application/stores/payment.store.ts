import { Injectable, signal, computed } from '@angular/core';
import { PaymentAdapter } from '../../infrastructure/payment.adapter';
import type { Payment } from '../../domain/payment.model';

/**
 * Payment store — manages payment state with signals.
 */
@Injectable({ providedIn: 'root' })
export class PaymentStore {
  private readonly adapter: PaymentAdapter;

  private readonly _payments = signal<Payment[]>([]);
  private readonly _selectedPayment = signal<Payment | null>(null);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _total = signal(0);

  readonly payments = this._payments.asReadonly();
  readonly selectedPayment = this._selectedPayment.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly total = this._total.asReadonly();
  readonly hasPayments = computed(() => this._payments().length > 0);

  constructor(adapter: PaymentAdapter) {
    this.adapter = adapter;
  }

  loadPayments(page = 0, size = 20): void {
    this._loading.set(true);
    this._error.set(null);
    this.adapter.list(page, size).subscribe({
      next: (res) => {
        this._payments.set(res.items);
        this._total.set(res.total);
        this._loading.set(false);
      },
      error: (err) => {
        this._error.set(err.detail || 'Failed to load payments');
        this._loading.set(false);
      },
    });
  }

  loadPayment(id: string): void {
    this._loading.set(true);
    this._error.set(null);
    this.adapter.getById(id).subscribe({
      next: (payment) => {
        this._selectedPayment.set(payment);
        this._loading.set(false);
      },
      error: (err) => {
        this._error.set(err.detail || 'Failed to load payment');
        this._loading.set(false);
      },
    });
  }

  processPayment(request: {
    accountId: string;
    userId: string;
    amount: { amount: string; currency: string };
    paymentType?: string;
    beneficiaryName?: string;
    beneficiaryDocumentType?: string;
    beneficiaryDocumentNumber?: string;
    beneficiaryAccountNumber?: string;
    beneficiaryBankCode?: string;
    beneficiaryBankName?: string;
    reference?: string;
    channel?: string;
  }): void {
    this._loading.set(true);
    this._error.set(null);
    const idempotencyKey = crypto.randomUUID();
    this.adapter.process(request, idempotencyKey).subscribe({
      next: (payment) => {
        this._payments.update((payments) => [payment, ...payments]);
        this._selectedPayment.set(payment);
        this._total.update((t) => t + 1);
        this._loading.set(false);
      },
      error: (err) => {
        this._error.set(err.detail || 'Failed to process payment');
        this._loading.set(false);
      },
    });
  }

  clearError(): void {
    this._error.set(null);
  }

  reset(): void {
    this._payments.set([]);
    this._selectedPayment.set(null);
    this._loading.set(false);
    this._error.set(null);
    this._total.set(0);
  }
}
