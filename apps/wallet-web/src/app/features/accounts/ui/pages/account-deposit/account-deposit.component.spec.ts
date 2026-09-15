import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AccountDepositComponent } from './account-deposit.component';
import { AccountStore } from '../../../application/stores/account.store';

describe('AccountDepositComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountDepositComponent],
      providers: [
        provideRouter([]),
        {
          provide: AccountStore,
          useValue: {
            loading: () => false,
            error: () => null,
            deposit: () => {},
          },
        },
      ],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(AccountDepositComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render deposit form', () => {
    const fixture = TestBed.createComponent(AccountDepositComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.form-card')).toBeTruthy();
  });

  it('should render amount input', () => {
    const fixture = TestBed.createComponent(AccountDepositComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('#amount')).toBeTruthy();
  });

  it('should render currency input', () => {
    const fixture = TestBed.createComponent(AccountDepositComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('#currency')).toBeTruthy();
  });

  it('should render deposit button', () => {
    const fixture = TestBed.createComponent(AccountDepositComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.btn-primary')?.textContent).toContain('Deposit');
  });

  it('should have default currency USD', () => {
    const fixture = TestBed.createComponent(AccountDepositComponent);
    expect(fixture.componentInstance.currency).toBe('USD');
  });
});
