import { PaymentStore } from './payment.store';
import { of, throwError } from 'rxjs';

describe('PaymentStore', () => {
  let store: PaymentStore;
  let adapterSpy: {
    list: ReturnType<typeof vi.fn>;
    getById: ReturnType<typeof vi.fn>;
    process: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    adapterSpy = {
      list: vi.fn(),
      getById: vi.fn(),
      process: vi.fn(),
    };
    store = new PaymentStore(adapterSpy as never);
  });

  it('should be created', () => {
    expect(store).toBeTruthy();
  });

  it('should start with empty state', () => {
    expect(store.payments()).toEqual([]);
    expect(store.selectedPayment()).toBeNull();
    expect(store.loading()).toBe(false);
    expect(store.error()).toBeNull();
    expect(store.total()).toBe(0);
    expect(store.hasPayments()).toBe(false);
  });

  describe('loadPayments', () => {
    it('should load payments list', () => {
      const mockPayments = [
        { id: 'pay-1', accountId: 'a1', userId: 'u1', amount: 10, currency: 'USD', status: 'COMPLETED', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
        { id: 'pay-2', accountId: 'a2', userId: 'u2', amount: 20, currency: 'USD', status: 'PENDING', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
      ];
      adapterSpy.list.mockReturnValue(of({ items: mockPayments, total: 2, page: 0, size: 20 }));

      store.loadPayments();

      expect(store.payments().length).toBe(2);
      expect(store.total()).toBe(2);
      expect(store.hasPayments()).toBe(true);
      expect(store.loading()).toBe(false);
    });

    it('should set error on failure', () => {
      adapterSpy.list.mockReturnValue(throwError(() => ({ detail: 'Server error' })));

      store.loadPayments();

      expect(store.error()).toBe('Server error');
      expect(store.loading()).toBe(false);
    });
  });

  describe('loadPayment', () => {
    it('should load single payment', () => {
      const mockPayment = { id: 'pay-1', accountId: 'a1', userId: 'u1', amount: 10, currency: 'USD', status: 'COMPLETED', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() };
      adapterSpy.getById.mockReturnValue(of(mockPayment));

      store.loadPayment('pay-1');

      expect(store.selectedPayment()?.id).toBe('pay-1');
      expect(store.loading()).toBe(false);
    });
  });

  describe('processPayment', () => {
    it('should add new payment to list', () => {
      const mockPayment = { id: 'pay-new', accountId: 'a1', userId: 'u1', amount: 10, currency: 'USD', status: 'PENDING', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() };
      adapterSpy.process.mockReturnValue(of(mockPayment));

      store.processPayment({ userId: 'u1', amount: { amount: '10', currency: 'USD' } });

      expect(store.payments().length).toBe(1);
      expect(store.payments()[0].id).toBe('pay-new');
      expect(store.total()).toBe(1);
    });
  });

  describe('reset', () => {
    it('should clear all state', () => {
      adapterSpy.list.mockReturnValue(of({ items: [{ id: 'p1' }], total: 1, page: 0, size: 20 }));
      store.loadPayments();
      expect(store.hasPayments()).toBe(true);

      store.reset();

      expect(store.payments()).toEqual([]);
      expect(store.total()).toBe(0);
      expect(store.hasPayments()).toBe(false);
    });
  });
});
