import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { MfaDisableComponent } from './mfa-disable.component';
import { AuthService } from '@core/infrastructure/auth.service';

function createValidToken(): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const futureExp = Math.floor(Date.now() / 1000) + 3600;
  const payload = btoa(JSON.stringify({ sub: 'user-1', exp: futureExp, iat: futureExp - 3600 }));
  return `${header}.${payload}.sig`;
}

describe('MfaDisableComponent', () => {
  let component: MfaDisableComponent;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, MfaDisableComponent],
      providers: [
        AuthService,
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    }).compileComponents();

    const auth = TestBed.inject(AuthService);
    auth.setToken(createValidToken());

    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    component = TestBed.inject(MfaDisableComponent);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should disable MFA and show success', () => {
    component.code = '123456';
    component.disableMfa();

    const req = httpMock.expectOne('/api/v1/auth/mfa/disable');
    expect(req.request.method).toBe('POST');
    req.flush(null, { status: 204, statusText: 'No Content' });

    expect(component.success).toBeTrue();
  });

  it('should show error on disable failure', () => {
    component.code = '000000';
    component.disableMfa();

    const req = httpMock.expectOne('/api/v1/auth/mfa/disable');
    req.flush({ message: 'Invalid code' }, { status: 400, statusText: 'Bad Request' });

    expect(component.errorMessage).toBe('Invalid code');
    expect(component.success).toBeFalse();
  });

  it('should navigate to home on cancel', () => {
    component.cancel();
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });
});
