import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { BaseAdapter } from '@core/infrastructure/base.adapter';
import type { Account, AccountPageResponse, OpenAccountRequest, DepositRequest, WithdrawRequest } from '../domain/account.model';

/**
 * Account HTTP adapter — consumes /api/v1/accounts.
 */
@Injectable({ providedIn: 'root' })
export class AccountAdapter extends BaseAdapter {
  private readonly accountHttp = inject(HttpClient);

  list(page = 0, size = 20, userId?: string): Observable<AccountPageResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (userId) {
      params = params.set('userId', userId);
    }

    return this.accountHttp.get<AccountPageResponse>(
      `${this.baseUrl}/api/v1/accounts`,
      { params }
    ).pipe(catchError(this.handleError));
  }

  open(request: OpenAccountRequest, idempotencyKey: string): Observable<Account> {
    return this.accountHttp.post<Account>(
      `${this.baseUrl}/api/v1/accounts`,
      request,
      { headers: { 'Idempotency-Key': idempotencyKey } }
    ).pipe(catchError(this.handleError));
  }

  getById(accountId: string): Observable<Account> {
    return this.accountHttp.get<Account>(
      `${this.baseUrl}/api/v1/accounts/${accountId}`
    ).pipe(catchError(this.handleError));
  }

  deposit(accountId: string, request: DepositRequest, idempotencyKey: string): Observable<Account> {
    return this.accountHttp.post<Account>(
      `${this.baseUrl}/api/v1/accounts/${accountId}/deposits`,
      request,
      { headers: { 'Idempotency-Key': idempotencyKey } }
    ).pipe(catchError(this.handleError));
  }

  withdraw(accountId: string, request: WithdrawRequest, idempotencyKey: string): Observable<Account> {
    return this.accountHttp.post<Account>(
      `${this.baseUrl}/api/v1/accounts/${accountId}/withdrawals`,
      request,
      { headers: { 'Idempotency-Key': idempotencyKey } }
    ).pipe(catchError(this.handleError));
  }
}
