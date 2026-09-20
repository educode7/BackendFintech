import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { DashboardComponent } from './dashboard.component';
import { AccountStore } from '@features/accounts/application/stores/account.store';
import { PaymentStore } from '@features/payments/application/stores/payment.store';
import { NotificationStore } from '@features/notifications/application/stores/notification.store';
import { AuthService } from '@core/infrastructure/auth.service';
import { signal } from '@angular/core';
import type { Account } from '@features/accounts/domain/account.model';
import type { Payment } from '@features/payments/domain/payment.model';
import type { Notification } from '@features/notifications/domain/notification.model';

describe('DashboardComponent', () => {
  const mockAccountStore = {
    accounts: signal<Account[]>([]),
    loading: signal(false),
    loadAccounts: vi.fn(),
  };

  const mockPaymentStore = {
    payments: signal<Payment[]>([]),
    loading: signal(false),
    loadPayments: vi.fn(),
  };

  const mockNotificationStore = {
    notifications: signal<Notification[]>([]),
    unreadCount: signal(0),
    loadNotifications: vi.fn(),
  };

  const mockAuthService = {
    getCachedUserInfo: vi.fn().mockReturnValue({ sub: 'test-user', email: 'test@example.com' }),
    getUserInfo: vi.fn().mockReturnValue({ sub: 'test-user', email: 'test@example.com' }),
  };

  beforeEach(async () => {
    mockAccountStore.loading.set(false);
    mockPaymentStore.loading.set(false);
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: AccountStore, useValue: mockAccountStore },
        { provide: PaymentStore, useValue: mockPaymentStore },
        { provide: NotificationStore, useValue: mockNotificationStore },
        { provide: AuthService, useValue: mockAuthService },
      ],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render page header', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-page-header')).toBeTruthy();
  });

  it('should render dashboard grid', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.dashboard-grid')).toBeTruthy();
  });

  it('should render stat cards', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const cards = compiled.querySelectorAll('.stat-card');
    expect(cards.length).toBe(3);
  });

  it('should load data on init', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    expect(mockAccountStore.loadAccounts).toHaveBeenCalled();
    expect(mockPaymentStore.loadPayments).toHaveBeenCalled();
  });

  it('should display account count', () => {
    mockAccountStore.accounts.set([
      { accountId: '1', userId: 'u1', balanceAmount: 100, balanceCurrency: 'USD', status: 'OPEN', version: 1, lastUpdated: '2024-01-01' },
      { accountId: '2', userId: 'u1', balanceAmount: 200, balanceCurrency: 'USD', status: 'OPEN', version: 1, lastUpdated: '2024-01-02' },
    ]);
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const statValues = compiled.querySelectorAll('.stat-value');
    expect(statValues[0]?.textContent).toContain('2');
  });

  it('should show spinner when loading', () => {
    mockAccountStore.loading.set(true);
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-loading-spinner')).toBeTruthy();
    expect(compiled.querySelector('.dashboard-grid')).toBeNull();
  });
});
