import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { PaymentListComponent } from './payment-list.component';
import { PaymentStore } from '../../../application/stores/payment.store';
import { AuthService } from '@core/infrastructure/auth.service';

describe('PaymentListComponent', () => {
  let authServiceMock: Partial<AuthService>;

  beforeEach(async () => {
    authServiceMock = {
      getUserInfo: vi.fn().mockReturnValue({ sub: 'test-user-id', email: 'test@test.com', roles: [], expiresAt: 0 }),
      getCachedUserInfo: vi.fn().mockReturnValue({ sub: 'test-user-id', email: 'test@test.com', roles: [], expiresAt: 0 }),
      isAuthenticated: vi.fn().mockReturnValue(true),
    };

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
        { provide: AuthService, useValue: authServiceMock },
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
