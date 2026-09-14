import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { PaymentDetailComponent } from './payment-detail.component';
import { PaymentService } from '@services/payment.service';

describe('PaymentDetailComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaymentDetailComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: () => 'pay-001',
              },
            },
          },
        },
        {
          provide: PaymentService,
          useValue: {
            getById: () => of({
              id: 'pay-001',
              userId: 'user-1',
              amount: { amount: '25.50', currency: 'USD' },
              status: 'COMPLETED',
              idempotencyKey: 'key-1',
              createdAt: new Date().toISOString(),
            }),
          },
        },
      ],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(PaymentDetailComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render payment details', () => {
    const fixture = TestBed.createComponent(PaymentDetailComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.detail-card')).toBeTruthy();
  });
});
