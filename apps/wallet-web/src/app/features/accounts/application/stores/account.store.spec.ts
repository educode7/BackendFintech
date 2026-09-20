import '@angular/compiler';
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
    list: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    adapterSpy = {
      getById: vi.fn(),
      open: vi.fn(),
      deposit: vi.fn(),
      withdraw: vi.fn(),
      list: vi.fn(),
    };
    store = new AccountStore(adapterSpy as unknown as AccountAdapter);
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
        balanceAmount: 100.00,
        balanceCurrency: 'USD',
        status: 'OPEN',
        version: 1,
        lastUpdated: new Date().toISOString(),
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
    it('should add new account to list and pass an idempotency key', () => {
      const mockAccount = {
        accountId: 'acc-2',
        userId: 'user-2',
        balanceAmount: 50.00,
        balanceCurrency: 'USD',
        status: 'OPEN',
        version: 1,
        lastUpdated: new Date().toISOString(),
      };
      adapterSpy.open.mockReturnValue(of(mockAccount));

      store.openAccount({ userId: 'user-2', initialBalance: { amount: '50.00', currency: 'USD' } });

      expect(adapterSpy.open).toHaveBeenCalledTimes(1);
      const [, idempotencyKey] = adapterSpy.open.mock.calls[0];
      expect(typeof idempotencyKey).toBe('string');
      expect(idempotencyKey).toMatch(
        /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i,
      );
      expect(store.accounts().length).toBe(1);
      expect(store.accounts()[0].accountId).toBe('acc-2');
      expect(store.hasAccounts()).toBe(true);
    });
  });

  describe('deposit', () => {
    it('should update account balance and pass an idempotency key', () => {
      const initial = {
        accountId: 'acc-1',
        userId: 'user-1',
        balanceAmount: 100.00,
        balanceCurrency: 'USD',
        status: 'OPEN',
        version: 1,
        lastUpdated: new Date().toISOString(),
      };
      const updated = { ...initial, balanceAmount: 150.00, version: 2 };

      adapterSpy.open.mockReturnValue(of(initial));
      store.openAccount({ userId: 'user-1', initialBalance: { amount: '100.00', currency: 'USD' } });

      adapterSpy.deposit.mockReturnValue(of(updated));
      store.deposit('acc-1', { amount: '50.00', currency: 'USD' });

      expect(adapterSpy.deposit).toHaveBeenCalledTimes(1);
      const [, , idempotencyKey] = adapterSpy.deposit.mock.calls[0];
      expect(typeof idempotencyKey).toBe('string');
      expect(idempotencyKey).toMatch(
        /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i,
      );
      expect(store.selectedAccount()?.balanceAmount).toBe(150.00);
      expect(store.accounts()[0].balanceAmount).toBe(150.00);
    });
  });

  describe('reset', () => {
    it('should clear all state', () => {
      adapterSpy.open.mockReturnValue(of({
        accountId: 'acc-1', userId: 'u', balanceAmount: 10, balanceCurrency: 'USD',
        status: 'OPEN', version: 1, lastUpdated: new Date().toISOString(),
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
