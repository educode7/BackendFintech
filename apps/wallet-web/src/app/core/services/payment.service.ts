import { inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { BaseService } from './base.service';
import type { Payment, ProcessPaymentRequest, PaymentPageResponse } from '@models/payment.model';

/**
 * Payment API service — consumes /api/v1/payments.
 */
export class PaymentService extends BaseService {
  private readonly paymentHttp = inject(HttpClient);

  process(request: ProcessPaymentRequest, idempotencyKey: string): Observable<Payment> {
    return this.paymentHttp.post<Payment>(
      `${this.baseUrl}/api/v1/payments`,
      request,
      { headers: { 'Idempotency-Key': idempotencyKey } }
    ).pipe(catchError(this.handleError));
  }

  getById(id: string): Observable<Payment> {
    return this.paymentHttp.get<Payment>(
      `${this.baseUrl}/api/v1/payments/${id}`
    ).pipe(catchError(this.handleError));
  }

  list(page = 0, size = 20): Observable<PaymentPageResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.paymentHttp.get<PaymentPageResponse>(
      `${this.baseUrl}/api/v1/payments`,
      { params }
    ).pipe(catchError(this.handleError));
  }
}
