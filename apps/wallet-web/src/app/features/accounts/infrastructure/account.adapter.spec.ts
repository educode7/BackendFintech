import '@angular/compiler';
import { Injector } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { of } from 'rxjs';
import { AccountAdapter } from './account.adapter';
import { environment } from '@env/environment';

function createAdapter(httpMock: HttpClient) {
  const injector = Injector.create({
    providers: [
      { provide: HttpClient, useValue: httpMock },
      AccountAdapter,
    ],
  });
  return injector.get(AccountAdapter);
}

describe('AccountAdapter', () => {
  let adapter: AccountAdapter;
  let httpGet: ReturnType<typeof vi.fn>;
  let httpPost: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    httpGet = vi.fn();
    httpPost = vi.fn();
    adapter = createAdapter({ get: httpGet, post: httpPost } as unknown as HttpClient);
  });

  it('should be created', () => {
    expect(adapter).toBeTruthy();
  });

  describe('open', () => {
    it('should POST to /api/v1/accounts with idempotency key', () => {
      const mockAccount = {
        accountId: 'acc-1',
        userId: 'user-1',
        balanceAmount: 100.00,
        balanceCurrency: 'USD',
        status: 'OPEN',
        version: 1,
        lastUpdated: new Date().toISOString(),
      };
      httpPost.mockReturnValue(of(mockAccount));

      const idempotencyKey = crypto.randomUUID();

      adapter.open(
        { userId: 'user-1', initialBalance: { amount: '100.00', currency: 'USD' } },
        idempotencyKey,
      ).subscribe((account) => {
        expect(account.accountId).toBe('acc-1');
      });

      expect(httpPost).toHaveBeenCalledOnce();
      const [url, body, opts] = httpPost.mock.calls[0];
      expect(url).toBe(`${environment.apiGateway}/api/v1/accounts`);
      expect(body).toEqual({ userId: 'user-1', initialBalance: { amount: '100.00', currency: 'USD' } });
      expect(opts.headers['Idempotency-Key']).toBe(idempotencyKey);
    });
  });

  describe('getById', () => {
    it('should GET /api/v1/accounts/:accountId', () => {
      const mockAccount = {
        accountId: 'acc-1',
        userId: 'user-1',
        balanceAmount: 100.00,
        balanceCurrency: 'USD',
        status: 'OPEN',
        version: 1,
        lastUpdated: new Date().toISOString(),
      };
      httpGet.mockReturnValue(of(mockAccount));

      adapter.getById('acc-1').subscribe((account) => {
        expect(account.accountId).toBe('acc-1');
      });

      expect(httpGet).toHaveBeenCalledOnce();
      expect(httpGet.mock.calls[0][0]).toBe(`${environment.apiGateway}/api/v1/accounts/acc-1`);
    });
  });

  describe('deposit', () => {
    it('should POST to /api/v1/accounts/:accountId/deposits with idempotency key', () => {
      const mockAccount = {
        accountId: 'acc-1',
        userId: 'user-1',
        balanceAmount: 150.00,
        balanceCurrency: 'USD',
        status: 'OPEN',
        version: 2,
        lastUpdated: new Date().toISOString(),
      };
      httpPost.mockReturnValue(of(mockAccount));

      const idempotencyKey = crypto.randomUUID();

      adapter.deposit('acc-1', { amount: '50.00', currency: 'USD' }, idempotencyKey).subscribe((account) => {
        expect(account.balanceAmount).toBe(150.00);
      });

      expect(httpPost).toHaveBeenCalledOnce();
      const [url, body, opts] = httpPost.mock.calls[0];
      expect(url).toBe(`${environment.apiGateway}/api/v1/accounts/acc-1/deposits`);
      expect(body).toEqual({ amount: '50.00', currency: 'USD' });
      expect(opts.headers['Idempotency-Key']).toBe(idempotencyKey);
    });
  });

  describe('withdraw', () => {
    it('should POST to /api/v1/accounts/:accountId/withdrawals with idempotency key', () => {
      const mockAccount = {
        accountId: 'acc-1',
        userId: 'user-1',
        balanceAmount: 50.00,
        balanceCurrency: 'USD',
        status: 'OPEN',
        version: 3,
        lastUpdated: new Date().toISOString(),
      };
      httpPost.mockReturnValue(of(mockAccount));

      const idempotencyKey = crypto.randomUUID();

      adapter.withdraw('acc-1', { amount: '50.00', currency: 'USD' }, idempotencyKey).subscribe((account) => {
        expect(account.balanceAmount).toBe(50.00);
      });

      expect(httpPost).toHaveBeenCalledOnce();
      const [url, body, opts] = httpPost.mock.calls[0];
      expect(url).toBe(`${environment.apiGateway}/api/v1/accounts/acc-1/withdrawals`);
      expect(body).toEqual({ amount: '50.00', currency: 'USD' });
      expect(opts.headers['Idempotency-Key']).toBe(idempotencyKey);
    });
  });

  describe('list', () => {
    it('should GET /api/v1/accounts with pagination params', () => {
      const mockResponse = {
        items: [],
        total: 0,
        page: 0,
        size: 20,
      };
      httpGet.mockReturnValue(of(mockResponse));

      adapter.list(0, 20).subscribe((response) => {
        expect(response.total).toBe(0);
      });

      expect(httpGet).toHaveBeenCalledOnce();
      const [url, opts] = httpGet.mock.calls[0];
      expect(url).toBe(`${environment.apiGateway}/api/v1/accounts`);
      expect(opts.params.get('page')).toBe('0');
      expect(opts.params.get('size')).toBe('20');
    });
  });
});
