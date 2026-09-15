import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PaymentDetailComponent } from './payment-detail.component';
import { PaymentStore } from '../../../application/stores/payment.store';

describe('PaymentDetailComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaymentDetailComponent],
      providers: [
        provideRouter([]),
        {
          provide: PaymentStore,
          useValue: {
            loading: () => false,
            error: () => null,
            selectedPayment: () => ({
              id: 'pay-001',
              userId: 'user-1',
              amount: { amount: '10.00', currency: 'USD' },
              status: 'COMPLETED',
              idempotencyKey: 'idem-1',
              createdAt: new Date().toISOString(),
            }),
            loadPayment: () => {},
          },
        },
      ],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(PaymentDetailComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render payment detail card', () => {
    const fixture = TestBed.createComponent(PaymentDetailComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.detail-card')).toBeTruthy();
  });
});
