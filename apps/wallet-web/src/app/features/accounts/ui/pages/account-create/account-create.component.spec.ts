import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { AccountCreateComponent } from './account-create.component';
import { AccountStore } from '../../../application/stores/account.store';
import { AuthService } from '@core/infrastructure/auth.service';

describe('AccountCreateComponent', () => {
  let openAccountSpy: ReturnType<typeof vi.fn>;
  let authServiceMock: Partial<AuthService>;

  beforeEach(async () => {
    openAccountSpy = vi.fn();
    authServiceMock = {
      getUserInfo: vi.fn().mockReturnValue({ sub: 'test-user-id', email: 'test@test.com', roles: [], expiresAt: 0 }),
      getCachedUserInfo: vi.fn().mockReturnValue({ sub: 'test-user-id', email: 'test@test.com', roles: [], expiresAt: 0 }),
      isAuthenticated: vi.fn().mockReturnValue(true),
    };

    await TestBed.configureTestingModule({
      imports: [AccountCreateComponent],
      providers: [
        provideRouter([]),
        {
          provide: AccountStore,
          useValue: {
            loading: () => false,
            error: () => null,
            openAccount: openAccountSpy,
          },
        },
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(AccountCreateComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render form card', () => {
    const fixture = TestBed.createComponent(AccountCreateComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.form-card')).toBeTruthy();
  });

  it('should render page header with title', () => {
    const fixture = TestBed.createComponent(AccountCreateComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-page-header')).toBeTruthy();
  });

  describe('Account Information section', () => {
    it('should render userId input', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#userId')).toBeTruthy();
    });

    it('should render accountType select', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      const select = compiled.querySelector('#accountType') as HTMLSelectElement;
      expect(select).toBeTruthy();
      expect(select.options.length).toBe(3);
      expect(select.options[0].value).toBe('SAVINGS');
      expect(select.options[1].value).toBe('CHECKING');
      expect(select.options[2].value).toBe('DEPOSIT');
    });

    it('should render currency input', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#currency')).toBeTruthy();
    });

    it('should render country input', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#country')).toBeTruthy();
    });

    it('should render initialAmount input', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#initialAmount')).toBeTruthy();
    });
  });

  describe('Banking Details section', () => {
    it('should render accountNumber input', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#accountNumber')).toBeTruthy();
    });

    it('should render CCI input', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#cci')).toBeTruthy();
    });

    it('should render IBAN input', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#iban')).toBeTruthy();
    });

    it('should render SWIFT/BIC input', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#swiftBic')).toBeTruthy();
    });

    it('should render bankCode input', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#bankCode')).toBeTruthy();
    });

    it('should render bankName input', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#bankName')).toBeTruthy();
    });
  });

  describe('Account Holder section', () => {
    it('should render holderName input', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#holderName')).toBeTruthy();
    });

    it('should render holderDocumentType select with LATAM options', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      const select = compiled.querySelector('#holderDocumentType') as HTMLSelectElement;
      expect(select).toBeTruthy();
      expect(select.options.length).toBe(7); // empty + 6 types
      expect(select.options[1].value).toBe('DNI');
      expect(select.options[2].value).toBe('RUC');
      expect(select.options[3].value).toBe('CE');
      expect(select.options[4].value).toBe('PASSPORT');
      expect(select.options[5].value).toBe('RFC');
      expect(select.options[6].value).toBe('CURP');
    });

    it('should render holderDocumentNumber input', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#holderDocumentNumber')).toBeTruthy();
    });

    it('should render holderEmail input', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#holderEmail')).toBeTruthy();
    });

    it('should render holderPhone input', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#holderPhone')).toBeTruthy();
    });
  });

  describe('Limits section', () => {
    it('should render dailyLimit input', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#dailyLimit')).toBeTruthy();
    });

    it('should render monthlyLimit input', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#monthlyLimit')).toBeTruthy();
    });

    it('should render singleTransactionLimit input', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#singleTransactionLimit')).toBeTruthy();
    });

    it('should render overdraftLimit input', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#overdraftLimit')).toBeTruthy();
    });
  });

  describe('Default values', () => {
    it('should have default accountType SAVINGS', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      expect(fixture.componentInstance.accountType).toBe('SAVINGS');
    });

    it('should have default currency USD', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      expect(fixture.componentInstance.currency).toBe('USD');
    });

    it('should have default country PE', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      expect(fixture.componentInstance.country).toBe('PE');
    });

    it('should have default initialCurrency USD', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      expect(fixture.componentInstance.initialCurrency).toBe('USD');
    });
  });

  describe('Validation', () => {
    it('should be invalid when userId is empty', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.componentInstance.userId = '';
      fixture.componentInstance.initialAmount = 100;
      expect(fixture.componentInstance.isValid()).toBe(false);
    });

    it('should be invalid when initialAmount is null', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.componentInstance.userId = 'user-001';
      fixture.componentInstance.initialAmount = null;
      expect(fixture.componentInstance.isValid()).toBe(false);
    });

    it('should be invalid when initialAmount is 0', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.componentInstance.userId = 'user-001';
      fixture.componentInstance.initialAmount = 0;
      expect(fixture.componentInstance.isValid()).toBe(false);
    });

    it('should be invalid when initialAmount is negative', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.componentInstance.userId = 'user-001';
      fixture.componentInstance.initialAmount = -100;
      expect(fixture.componentInstance.isValid()).toBe(false);
    });

    it('should be valid when userId and initialAmount are provided', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.componentInstance.userId = 'user-001';
      fixture.componentInstance.initialAmount = 100;
      expect(fixture.componentInstance.isValid()).toBe(true);
    });
  });

  describe('Submit', () => {
    it('should call store.openAccount with correct request', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      const comp = fixture.componentInstance;

      comp.userId = 'user-001';
      comp.initialAmount = 1000;
      comp.accountType = 'CHECKING';
      comp.currency = 'PEN';
      comp.country = 'PE';
      comp.holderName = 'Juan Perez';
      comp.holderDocumentType = 'DNI';
      comp.holderDocumentNumber = '12345678';
      comp.iban = 'PE123456789';
      comp.swiftBic = 'BCONPEPL';

      comp.submit();

      expect(openAccountSpy).toHaveBeenCalledWith({
        userId: 'user-001',
        initialBalance: { amount: '1000', currency: 'USD' },
        accountType: 'CHECKING',
        currency: 'PEN',
        country: 'PE',
        accountNumber: undefined,
        cci: undefined,
        iban: 'PE123456789',
        swiftBic: 'BCONPEPL',
        bankCode: undefined,
        bankName: undefined,
        holderName: 'Juan Perez',
        holderDocumentType: 'DNI',
        holderDocumentNumber: '12345678',
        holderEmail: undefined,
        holderPhone: undefined,
        dailyLimit: undefined,
        monthlyLimit: undefined,
        singleTransactionLimit: undefined,
        overdraftLimit: undefined,
      });
    });

    it('should not call store.openAccount when invalid', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.componentInstance.userId = '';
      fixture.componentInstance.initialAmount = null;

      fixture.componentInstance.submit();

      expect(openAccountSpy).not.toHaveBeenCalled();
    });
  });

  describe('Buttons', () => {
    it('should render Create Account button', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      const btn = compiled.querySelector('.btn-primary');
      expect(btn?.textContent).toContain('Create Account');
    });

    it('should render Cancel button', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      const btn = compiled.querySelector('.btn-secondary');
      expect(btn?.textContent).toContain('Cancel');
    });

    it('should disable Create button when form is invalid', () => {
      const fixture = TestBed.createComponent(AccountCreateComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      const btn = compiled.querySelector('.btn-primary') as HTMLButtonElement;
      expect(btn.disabled).toBe(true);
    });
  });
});
