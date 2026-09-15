import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PaymentListComponent } from './payment-list.component';
import { PaymentStore } from '../../../application/stores/payment.store';

describe('PaymentListComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaymentListComponent],
      providers: [
        provideRouter([]),
        {
          provide: PaymentStore,
          useValue: {
            loading: () => false,
            error: () => null,
            payments: () => [],
            loadPayments: () => {},
          },
        },
      ],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(PaymentListComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render page header', () => {
    const fixture = TestBed.createComponent(PaymentListComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-page-header')).toBeTruthy();
  });

  it('should render new payment button', () => {
    const fixture = TestBed.createComponent(PaymentListComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.btn-primary')?.textContent).toContain('New Payment');
  });

  it('should render table headers', () => {
    const fixture = TestBed.createComponent(PaymentListComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.data-table')).toBeTruthy();
  });
});
