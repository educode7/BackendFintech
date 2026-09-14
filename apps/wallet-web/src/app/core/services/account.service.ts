import { inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { BaseService } from './base.service';
import type { Account, OpenAccountRequest, DepositRequest, WithdrawRequest } from '@models/account.model';

/**
 * Account API service — consumes /api/v1/accounts.
 */
export class AccountService extends BaseService {
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
