import { TestBed } from '@angular/core/testing';
import { AccountStore } from './account.store';
import { AccountAdapter } from '../../infrastructure/account.adapter';
import { of, throwError } from 'rxjs';

describe('AccountStore', () => {
  let store: AccountStore;
  let adapterSpy: {
    getById: ReturnType<typeof vi.fn>;
    open: ReturnType<typeof vi.fn>;
    deposit: ReturnType<typeof vi.fn>;
    withdraw: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    adapterSpy = {
      getById: vi.fn(),
      open: vi.fn(),
      deposit: vi.fn(),
      withdraw: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        AccountStore,
        { provide: AccountAdapter, useValue: adapterSpy },
      ],
    });

    store = TestBed.inject(AccountStore);
  });

  it('should be created', () => {
    expect(store).toBeTruthy();
  });

  it('should start with empty state', () => {
    expect(store.accounts()).toEqual([]);
    expect(store.selectedAccount()).toBeNull();
    expect(store.loading()).toBe(false);
    expect(store.error()).toBeNull();
    expect(store.hasAccounts()).toBe(false);
  });

  describe('loadAccount', () => {
    it('should load account by id', () => {
      const mockAccount = {
        accountId: 'acc-1',
        userId: 'user-1',
        balance: { amount: '100.00', currency: 'USD' },
        version: 1,
        createdAt: new Date().toISOString(),
      };
      adapterSpy.getById.mockReturnValue(of(mockAccount));

      store.loadAccount('acc-1');

      expect(store.selectedAccount()).toEqual(mockAccount);
      expect(store.loading()).toBe(false);
      expect(store.error()).toBeNull();
    });

    it('should set error on failure', () => {
      adapterSpy.getById.mockReturnValue(throwError(() => ({ detail: 'Not found' })));

      store.loadAccount('acc-999');

      expect(store.error()).toBe('Not found');
      expect(store.loading()).toBe(false);
    });
  });

  describe('openAccount', () => {
    it('should add new account to list', () => {
      const mockAccount = {
        accountId: 'acc-2',
        userId: 'user-2',
        balance: { amount: '50.00', currency: 'USD' },
        version: 1,
        createdAt: new Date().toISOString(),
      };
      adapterSpy.open.mockReturnValue(of(mockAccount));

      store.openAccount({ userId: 'user-2', initialBalance: { amount: '50.00', currency: 'USD' } });

      expect(store.accounts().length).toBe(1);
      expect(store.accounts()[0].accountId).toBe('acc-2');
      expect(store.hasAccounts()).toBe(true);
    });
  });

  describe('deposit', () => {
    it('should update account balance', () => {
      const initial = {
        accountId: 'acc-1',
        userId: 'user-1',
        balance: { amount: '100.00', currency: 'USD' },
        version: 1,
        createdAt: new Date().toISOString(),
      };
      const updated = { ...initial, balance: { amount: '150.00', currency: 'USD' }, version: 2 };

      adapterSpy.open.mockReturnValue(of(initial));
      store.openAccount({ userId: 'user-1', initialBalance: { amount: '100.00', currency: 'USD' } });

      adapterSpy.deposit.mockReturnValue(of(updated));
      store.deposit('acc-1', { amount: '50.00', currency: 'USD' });

      expect(store.selectedAccount()?.balance.amount).toBe('150.00');
      expect(store.accounts()[0].balance.amount).toBe('150.00');
    });
  });

  describe('reset', () => {
    it('should clear all state', () => {
      adapterSpy.open.mockReturnValue(of({
        accountId: 'acc-1', userId: 'u', balance: { amount: '10', currency: 'USD' },
        version: 1, createdAt: new Date().toISOString(),
      }));
      store.openAccount({ userId: 'u', initialBalance: { amount: '10', currency: 'USD' } });
      expect(store.hasAccounts()).toBe(true);

      store.reset();

      expect(store.accounts()).toEqual([]);
      expect(store.selectedAccount()).toBeNull();
      expect(store.hasAccounts()).toBe(false);
    });
  });

  describe('clearError', () => {
    it('should clear error state', () => {
      adapterSpy.getById.mockReturnValue(throwError(() => ({ detail: 'Error' })));
      store.loadAccount('acc-1');
      expect(store.error()).toBe('Error');

      store.clearError();
      expect(store.error()).toBeNull();
    });
  });
});
