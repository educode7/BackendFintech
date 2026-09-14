import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PaymentService } from './payment.service';
import { environment } from '@env/environment';

describe('PaymentService', () => {
  let service: PaymentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PaymentService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(PaymentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
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

      service.process(
        { userId: 'user-1', amount: { amount: '10.00', currency: 'USD' } },
        'idem-1'
      ).subscribe((payment) => {
        expect(payment.id).toBe('pay-1');
        expect(payment.status).toBe('PENDING');
      });

      const req = httpMock.expectOne(`${environment.apiGateway}/api/v1/payments`);
      expect(req.request.method).toBe('POST');
      expect(req.request.headers.get('Idempotency-Key')).toBe('idem-1');
      req.flush(mockPayment);
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

      service.getById('pay-1').subscribe((payment) => {
        expect(payment.id).toBe('pay-1');
        expect(payment.status).toBe('COMPLETED');
      });

      const req = httpMock.expectOne(`${environment.apiGateway}/api/v1/payments/pay-1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockPayment);
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

      service.list(0, 20).subscribe((response) => {
        expect(response.total).toBe(0);
      });

      const req = httpMock.expectOne(
        (r) => r.url === `${environment.apiGateway}/api/v1/payments`
      );
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('20');
      req.flush(mockResponse);
    });
  });
});
