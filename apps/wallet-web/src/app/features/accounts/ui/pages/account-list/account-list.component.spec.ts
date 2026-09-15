import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AccountListComponent } from './account-list.component';
import { AccountStore } from '../../../application/stores/account.store';

describe('AccountListComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountListComponent],
      providers: [
        provideRouter([]),
        {
          provide: AccountStore,
          useValue: {
            loading: () => false,
            error: () => null,
            accounts: () => [],
            loadAccounts: () => {},
          },
        },
      ],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(AccountListComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render page header', () => {
    const fixture = TestBed.createComponent(AccountListComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-page-header')).toBeTruthy();
  });

  it('should render new account button', () => {
    const fixture = TestBed.createComponent(AccountListComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.btn-primary')?.textContent).toContain('New Account');
  });
});
