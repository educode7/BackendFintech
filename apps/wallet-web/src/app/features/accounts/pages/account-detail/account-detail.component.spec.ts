import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { AccountDetailComponent } from './account-detail.component';
import { AccountService } from '@services/account.service';

describe('AccountDetailComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountDetailComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: () => 'acc-001',
              },
            },
          },
        },
        {
          provide: AccountService,
          useValue: {
            getById: () => of({
              accountId: 'acc-001',
              userId: 'user-1',
              balance: { amount: '100.00', currency: 'USD' },
              status: 'OPEN',
              version: 1,
              createdAt: new Date().toISOString(),
            }),
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
