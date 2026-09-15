import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { BaseAdapter } from '@core/infrastructure/base.adapter';
import type { Account, OpenAccountRequest, DepositRequest, WithdrawRequest } from '../domain/account.model';

/**
 * Account HTTP adapter — consumes /api/v1/accounts.
 */
@Injectable({ providedIn: 'root' })
export class AccountAdapter extends BaseAdapter {
  private readonly accountHttp = inject(HttpClient);

  open(request: OpenAccountRequest): Observable<Account> {
    return this.accountHttp.post<Account>(
      `${this.baseUrl}/api/v1/accounts`,
      request
    ).pipe(catchError(this.handleError));
  }

  getById(accountId: string): Observable<Account> {
    return this.accountHttp.get<Account>(
      `${this.baseUrl}/api/v1/accounts/${accountId}`
    ).pipe(catchError(this.handleError));
  }

  deposit(accountId: string, request: DepositRequest): Observable<Account> {
    return this.accountHttp.post<Account>(
      `${this.baseUrl}/api/v1/accounts/${accountId}/deposits`,
      request
    ).pipe(catchError(this.handleError));
  }

  withdraw(accountId: string, request: WithdrawRequest): Observable<Account> {
    return this.accountHttp.post<Account>(
      `${this.baseUrl}/api/v1/accounts/${accountId}/withdrawals`,
      request
    ).pipe(catchError(this.handleError));
  }
}
