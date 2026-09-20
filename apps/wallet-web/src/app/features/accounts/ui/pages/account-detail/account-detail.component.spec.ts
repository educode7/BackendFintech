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
              accountId: 'acc-001',
              userId: 'user-1',
              balanceAmount: 100.00,
              balanceCurrency: 'USD',
              status: 'OPEN',
              version: 1,
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
    expect(compiled.querySelector('.balance')).toBeTruthy();
  });
});
