import '@angular/compiler';
import { Injector, runInInjectionContext } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { AuthAdapter } from './auth.adapter';
import { AuthService } from '@core/infrastructure/auth.service';
import { environment } from '@env/environment';

function createAdapter(httpMock: HttpClient, authMock: AuthService) {
  const injector = Injector.create({
    providers: [
      { provide: HttpClient, useValue: httpMock },
      { provide: AuthService, useValue: authMock },
      AuthAdapter,
    ],
  });
  return injector.get(AuthAdapter);
}

describe('AuthAdapter', () => {
  let adapter: AuthAdapter;
  let httpPost: ReturnType<typeof vi.fn>;
  let authGetToken: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    httpPost = vi.fn();
    authGetToken = vi.fn().mockReturnValue('test-token');
    adapter = createAdapter(
      { post: httpPost } as unknown as HttpClient,
      { getToken: authGetToken } as unknown as AuthService,
    );
  });

  it('should be created', () => {
    expect(adapter).toBeTruthy();
  });

  describe('setup', () => {
    it('should POST to /api/v1/auth/mfa/setup with Bearer token', () => {
      const mockResponse = {
        qr_code: 'data:image/png;base64,abc',
        secret: 'SECRET123',
        recovery_codes: ['code1', 'code2'],
        issuer: 'WalletApp',
        account_name: 'user@test.com',
      };
      httpPost.mockReturnValue(of(mockResponse));

      adapter.setup().subscribe((data) => {
        expect(data.secret).toBe('SECRET123');
        expect(data.recovery_codes).toEqual(['code1', 'code2']);
      });

      expect(httpPost).toHaveBeenCalledOnce();
      const [url, body, opts] = httpPost.mock.calls[0];
      expect(url).toBe(`${environment.apiGateway}/api/v1/auth/mfa/setup`);
      expect(body).toEqual({});
      expect(opts.headers.get('Authorization')).toBe('Bearer test-token');
    });
  });

  describe('verify', () => {
    it('should POST to /api/v1/auth/mfa/verify with code and Bearer token', () => {
      const mockResponse = {
        verified: true,
        backup_codes_remaining: 8,
      };
      httpPost.mockReturnValue(of(mockResponse));

      adapter.verify('123456').subscribe((data) => {
        expect(data.verified).toBe(true);
        expect(data.backup_codes_remaining).toBe(8);
      });

      expect(httpPost).toHaveBeenCalledOnce();
      const [url, body, opts] = httpPost.mock.calls[0];
      expect(url).toBe(`${environment.apiGateway}/api/v1/auth/mfa/verify`);
      expect(body).toEqual({ code: '123456' });
      expect(opts.headers.get('Authorization')).toBe('Bearer test-token');
    });
  });

  describe('disable', () => {
    it('should POST to /api/v1/auth/mfa/disable with code and Bearer token', () => {
      const mockResponse = { status: 204 };
      httpPost.mockReturnValue(of(mockResponse));

      adapter.disable('123456').subscribe((data) => {
        expect(data).toBeTruthy();
      });

      expect(httpPost).toHaveBeenCalledOnce();
      const [url, body, opts] = httpPost.mock.calls[0];
      expect(url).toBe(`${environment.apiGateway}/api/v1/auth/mfa/disable`);
      expect(body).toEqual({ code: '123456' });
      expect(opts.headers.get('Authorization')).toBe('Bearer test-token');
    });
  });
});
