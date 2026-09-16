import '@angular/compiler';
import { Injector, runInInjectionContext } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { of } from 'rxjs';
import { PaymentAdapter } from './payment.adapter';
import { environment } from '@env/environment';

function createAdapter(httpMock: HttpClient) {
  const injector = Injector.create({
    providers: [
      { provide: HttpClient, useValue: httpMock },
      PaymentAdapter,
    ],
  });
  return injector.get(PaymentAdapter);
}

describe('PaymentAdapter', () => {
  let adapter: PaymentAdapter;
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

  describe('process', () => {
    it('should POST to /api/v1/payments with idempotency key', () => {
      const mockPayment = {
        id: 'pay-1',
        userId: 'user-1',
        amount: { amount: '10.00', currency: 'USD' },
        status: 'PENDING',
        idempotencyKey: 'idem-1',
        createdAt: new Date().toISOString(),
      };
      httpPost.mockReturnValue(of(mockPayment));

      adapter.process(
        { userId: 'user-1', amount: { amount: '10.00', currency: 'USD' } },
        'idem-1',
      ).subscribe((payment) => {
        expect(payment.id).toBe('pay-1');
        expect(payment.status).toBe('PENDING');
      });

      expect(httpPost).toHaveBeenCalledOnce();
      const [url, body, opts] = httpPost.mock.calls[0];
      expect(url).toBe(`${environment.apiGateway}/api/v1/payments`);
      expect(body).toEqual({ userId: 'user-1', amount: { amount: '10.00', currency: 'USD' } });
      expect(opts.headers['Idempotency-Key']).toBe('idem-1');
    });
  });

  describe('getById', () => {
    it('should GET /api/v1/payments/:id', () => {
      const mockPayment = {
        id: 'pay-1',
        userId: 'user-1',
        amount: { amount: '10.00', currency: 'USD' },
        status: 'COMPLETED',
        idempotencyKey: 'idem-1',
        createdAt: new Date().toISOString(),
      };
      httpGet.mockReturnValue(of(mockPayment));

      adapter.getById('pay-1').subscribe((payment) => {
        expect(payment.id).toBe('pay-1');
        expect(payment.status).toBe('COMPLETED');
      });

      expect(httpGet).toHaveBeenCalledOnce();
      expect(httpGet.mock.calls[0][0]).toBe(`${environment.apiGateway}/api/v1/payments/pay-1`);
    });
  });

  describe('list', () => {
    it('should GET /api/v1/payments with pagination params', () => {
      const mockResponse = {
        data: [],
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
      expect(url).toBe(`${environment.apiGateway}/api/v1/payments`);
      expect(opts.params.get('page')).toBe('0');
      expect(opts.params.get('size')).toBe('20');
    });
  });
});
