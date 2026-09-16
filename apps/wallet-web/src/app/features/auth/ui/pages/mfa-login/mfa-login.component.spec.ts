import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { MfaLoginComponent } from './mfa-login.component';
import { AuthService } from '@core/infrastructure/auth.service';

function createValidToken(): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const futureExp = Math.floor(Date.now() / 1000) + 3600;
  const payload = btoa(JSON.stringify({ sub: 'user-1', exp: futureExp, iat: futureExp - 3600 }));
  return `${header}.${payload}.sig`;
}

describe('MfaLoginComponent', () => {
  let component: MfaLoginComponent;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, MfaLoginComponent],
      providers: [
        AuthService,
        { provide: Router, useValue: { navigate: jasmine.createSpy('navigate') } },
      ],
    }).compileComponents();

    const auth = TestBed.inject(AuthService);
    auth.setToken(createValidToken());

    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    component = TestBed.inject(MfaLoginComponent);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should verify code and navigate on success', () => {
    component.code = '123456';
    component.verify();

    const req = httpMock.expectOne('/api/v1/auth/mfa/verify');
    expect(req.request.body).toEqual({ code: '123456' });
    req.flush({ verified: true, backup_codes_remaining: 10 });

    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should increment attempts on failure', () => {
    component.code = '000000';
    component.verify();

    const req = httpMock.expectOne('/api/v1/auth/mfa/verify');
    req.flush({ verified: false, backup_codes_remaining: 10 });

    expect(component.attempts).toBe(1);
    expect(component.errorMessage).toBeTruthy();
  });

  it('should lock after max attempts', () => {
    for (let i = 0; i < 5; i++) {
      component.code = '000000';
      component.verify();

      const req = httpMock.expectOne('/api/v1/auth/mfa/verify');
      req.flush({ verified: false, backup_codes_remaining: 10 });
    }

    expect(component.locked).toBeTrue();
    expect(component.remainingSeconds).toBe(300);
  });

  it('should format time correctly', () => {
    expect(component.formatTime(0)).toBe('0:00');
    expect(component.formatTime(65)).toBe('1:05');
    expect(component.formatTime(300)).toBe('5:00');
  });
});
