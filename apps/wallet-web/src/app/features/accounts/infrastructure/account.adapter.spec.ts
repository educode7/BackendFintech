import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AccountAdapter } from './account.adapter';
import { environment } from '@env/environment';

describe('AccountAdapter', () => {
  let adapter: AccountAdapter;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AccountAdapter,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    adapter = TestBed.inject(AccountAdapter);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(adapter).toBeTruthy();
  });

  describe('open', () => {
    it('should POST to /api/v1/accounts', () => {
      const mockAccount = {
        accountId: 'acc-1',
        userId: 'user-1',
        balance: { amount: '100.00', currency: 'USD' },
        version: 1,
        createdAt: new Date().toISOString(),
      };

      adapter.open({
        userId: 'user-1',
        initialBalance: { amount: '100.00', currency: 'USD' },
      }).subscribe((account) => {
        expect(account.accountId).toBe('acc-1');
      });

      const req = httpMock.expectOne(`${environment.apiGateway}/api/v1/accounts`);
      expect(req.request.method).toBe('POST');
      req.flush(mockAccount);
    });
  });

  describe('getById', () => {
    it('should GET /api/v1/accounts/:accountId', () => {
      const mockAccount = {
        accountId: 'acc-1',
        userId: 'user-1',
        balance: { amount: '100.00', currency: 'USD' },
        version: 1,
        createdAt: new Date().toISOString(),
      };

      adapter.getById('acc-1').subscribe((account) => {
        expect(account.accountId).toBe('acc-1');
      });

      const req = httpMock.expectOne(`${environment.apiGateway}/api/v1/accounts/acc-1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockAccount);
    });
  });

  describe('deposit', () => {
    it('should POST to /api/v1/accounts/:accountId/deposits', () => {
      const mockAccount = {
        accountId: 'acc-1',
        userId: 'user-1',
        balance: { amount: '150.00', currency: 'USD' },
        version: 2,
        createdAt: new Date().toISOString(),
      };

      adapter.deposit('acc-1', { amount: '50.00', currency: 'USD' }).subscribe((account) => {
        expect(account.balance.amount).toBe('150.00');
      });

      const req = httpMock.expectOne(`${environment.apiGateway}/api/v1/accounts/acc-1/deposits`);
      expect(req.request.method).toBe('POST');
      req.flush(mockAccount);
    });
  });

  describe('withdraw', () => {
    it('should POST to /api/v1/accounts/:accountId/withdrawals', () => {
      const mockAccount = {
        accountId: 'acc-1',
        userId: 'user-1',
        balance: { amount: '50.00', currency: 'USD' },
        version: 3,
        createdAt: new Date().toISOString(),
      };

      adapter.withdraw('acc-1', { amount: '50.00', currency: 'USD' }).subscribe((account) => {
        expect(account.balance.amount).toBe('50.00');
      });

      const req = httpMock.expectOne(`${environment.apiGateway}/api/v1/accounts/acc-1/withdrawals`);
      expect(req.request.method).toBe('POST');
      req.flush(mockAccount);
    });
  });
});
