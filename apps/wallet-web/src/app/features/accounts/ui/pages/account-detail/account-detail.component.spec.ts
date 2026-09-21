import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AccountDetailComponent } from './account-detail.component';
import { AccountStore } from '../../../application/stores/account.store';

describe('AccountDetailComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountDetailComponent],
      providers: [
        provideRouter([]),
        {
          provide: AccountStore,
          useValue: {
            loading: () => false,
            error: () => null,
            selectedAccount: () => ({
              accountId: 'acc-001', accountNumber: '001', accountType: 'SAVINGS', cci: '', iban: '', swiftBic: '',
              userId: 'user-1', holderName: 'Test User', holderDocumentType: 'DNI', holderDocumentNumber: '123',
              holderEmail: '', holderPhone: '', bankCode: '', bankName: '', currency: 'USD', country: 'US',
              balanceAmount: 100.00, balanceCurrency: 'USD',
              availableAmount: 100, availableAmountCurrency: 'USD',
              holdAmount: 0, holdAmountCurrency: 'USD',
              overdraftLimit: 0, overdraftLimitCurrency: 'USD',
              dailyLimit: 0, dailyLimitCurrency: 'USD',
              monthlyLimit: 0, monthlyLimitCurrency: 'USD',
              singleTransactionLimit: 0, singleTransactionLimitCurrency: 'USD',
              status: 'OPEN', activatedAt: '', closedAt: '', version: 1,
              lastUpdated: new Date().toISOString(),
            }),
            loadAccount: () => {},
          },
        },
      ],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(AccountDetailComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render account balance', () => {
    const fixture = TestBed.createComponent(AccountDetailComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.balance-card')).toBeTruthy();
  });
});
