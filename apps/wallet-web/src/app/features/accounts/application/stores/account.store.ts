import { Injectable, signal, computed } from '@angular/core';
import { AccountAdapter } from '../../infrastructure/account.adapter';
import type { Account } from '../../domain/account.model';

/**
 * Account store — manages account state with signals.
 * Components interact with this instead of the adapter directly.
 */
@Injectable({ providedIn: 'root' })
export class AccountStore {
  private readonly adapter: AccountAdapter;

  private readonly _accounts = signal<Account[]>([]);
  private readonly _selectedAccount = signal<Account | null>(null);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly accounts = this._accounts.asReadonly();
  readonly selectedAccount = this._selectedAccount.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly hasAccounts = computed(() => this._accounts().length > 0);

  constructor(adapter: AccountAdapter) {
    this.adapter = adapter;
  }

  loadAccount(accountId: string): void {
    this._loading.set(true);
    this._error.set(null);
    this.adapter.getById(accountId).subscribe({
      next: (account) => {
        this._selectedAccount.set(account);
        this._loading.set(false);
      },
      error: (err) => {
        this._error.set(err.detail || 'Failed to load account');
        this._loading.set(false);
      },
    });
  }

  openAccount(request: { userId: string; initialBalance: { amount: string; currency: string } }): void {
    this._loading.set(true);
    this._error.set(null);
    this.adapter.open(request).subscribe({
      next: (account) => {
        this._accounts.update((accounts) => [...accounts, account]);
        this._selectedAccount.set(account);
        this._loading.set(false);
      },
      error: (err) => {
        this._error.set(err.detail || 'Failed to open account');
        this._loading.set(false);
      },
    });
  }

  deposit(accountId: string, request: { amount: string; currency: string }): void {
    this._loading.set(true);
    this._error.set(null);
    this.adapter.deposit(accountId, request).subscribe({
      next: (account) => {
        this._selectedAccount.set(account);
        this._accounts.update((accounts) =>
          accounts.map((a) => (a.accountId === accountId ? account : a))
        );
        this._loading.set(false);
      },
      error: (err) => {
        this._error.set(err.detail || 'Failed to deposit');
        this._loading.set(false);
      },
    });
  }

  withdraw(accountId: string, request: { amount: string; currency: string }): void {
    this._loading.set(true);
    this._error.set(null);
    this.adapter.withdraw(accountId, request).subscribe({
      next: (account) => {
        this._selectedAccount.set(account);
        this._accounts.update((accounts) =>
          accounts.map((a) => (a.accountId === accountId ? account : a))
        );
        this._loading.set(false);
      },
      error: (err) => {
        this._error.set(err.detail || 'Failed to withdraw');
        this._loading.set(false);
      },
    });
  }

  clearError(): void {
    this._error.set(null);
  }

  reset(): void {
    this._accounts.set([]);
    this._selectedAccount.set(null);
    this._loading.set(false);
    this._error.set(null);
  }
}
