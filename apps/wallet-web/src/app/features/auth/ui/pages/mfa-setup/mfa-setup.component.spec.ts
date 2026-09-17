import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { MfaSetupComponent } from './mfa-setup.component';
import { AuthService } from '@core/infrastructure/auth.service';

function createValidToken(): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const futureExp = Math.floor(Date.now() / 1000) + 3600;
  const payload = btoa(JSON.stringify({ sub: 'user-1', exp: futureExp, iat: futureExp - 3600 }));
  return `${header}.${payload}.sig`;
}

describe('MfaSetupComponent', () => {
  let component: MfaSetupComponent;
  let httpMock: HttpTestingController;
  let auth: AuthService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, MfaSetupComponent],
      providers: [
        AuthService,
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    }).compileComponents();

    auth = TestBed.inject(AuthService);
    auth.setToken(createValidToken());

    httpMock = TestBed.inject(HttpTestingController);
    component = TestBed.inject(MfaSetupComponent);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load setup data on init', () => {
    component.ngOnInit();

    const req = httpMock.expectOne('/api/v1/auth/mfa/setup');
    expect(req.request.method).toBe('POST');
    req.flush({
      qr_code: 'data:image/png;base64,abc',
      secret: 'SECRET',
      recovery_codes: ['c1', 'c2'],
      issuer: 'WalletApp',
      account_name: 'user@test.com',
    });

    expect(component.setupData).toBeTruthy();
    expect(component.setupData!.secret).toBe('SECRET');
    expect(component.loading).toBeFalse();
  });

  it('should show error on setup failure', () => {
    component.ngOnInit();

    const req = httpMock.expectOne('/api/v1/auth/mfa/setup');
    req.flush({ message: 'Setup failed' }, { status: 500, statusText: 'Error' });

    expect(component.error).toBe('Setup failed');
    expect(component.loading).toBeFalse();
  });

  it('should verify code successfully', () => {
    component.ngOnInit();

    const setupReq = httpMock.expectOne('/api/v1/auth/mfa/setup');
    setupReq.flush({
      qr_code: 'data:image/png;base64,abc',
      secret: 'SECRET',
      recovery_codes: ['c1', 'c2'],
      issuer: 'WalletApp',
      account_name: 'user@test.com',
    });

    component.verifyCode = '123456';
    component.verifySetup();

    const verifyReq = httpMock.expectOne('/api/v1/auth/mfa/verify');
    expect(verifyReq.request.body).toEqual({ code: '123456' });
    verifyReq.flush({ verified: true, backup_codes_remaining: 10 });

    expect(component.setupComplete).toBeTrue();
  });
});
