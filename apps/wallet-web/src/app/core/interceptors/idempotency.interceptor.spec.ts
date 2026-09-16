import '@angular/compiler';
import { Injector, runInInjectionContext } from '@angular/core';
import { HttpHeaders, HttpRequest, HttpResponse } from '@angular/common/http';
import { firstValueFrom, of } from 'rxjs';
import { LoggerService } from '@core/infrastructure/logger.service';
import { idempotencyInterceptor } from './idempotency.interceptor';

const mockLogger = {
  debug: vi.fn(),
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
} as unknown as LoggerService;

const injector = Injector.create({
  providers: [{ provide: LoggerService, useValue: mockLogger }],
});

function callInterceptor(req: HttpRequest<unknown>) {
  const next = (_r: HttpRequest<unknown>) =>
    of(new HttpResponse({ body: {}, status: 200 }));
  return runInInjectionContext(injector, () =>
    idempotencyInterceptor(req, next),
  );
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('idempotencyInterceptor', () => {
  describe('when Idempotency-Key is already present', () => {
    it('should preserve the key on POST requests', async () => {
      const req = new HttpRequest('POST', '/api/v1/payments', {}, {
        headers: new HttpHeaders({ 'Idempotency-Key': 'test-key-123' }),
      });

      const event = await firstValueFrom(callInterceptor(req));
      expect(event).toBeInstanceOf(HttpResponse);
      expect(mockLogger.debug).toHaveBeenCalledOnce();
    });

    it('should preserve the key on PUT requests', async () => {
      const req = new HttpRequest('PUT', '/api/v1/accounts/1', {}, {
        headers: new HttpHeaders({ 'Idempotency-Key': 'test-key-456' }),
      });

      const event = await firstValueFrom(callInterceptor(req));
      expect(event).toBeInstanceOf(HttpResponse);
      expect(mockLogger.debug).toHaveBeenCalledOnce();
    });

    it('should preserve the key on PATCH requests', async () => {
      const req = new HttpRequest('PATCH', '/api/v1/accounts/1', {}, {
        headers: new HttpHeaders({ 'Idempotency-Key': 'test-key-789' }),
      });

      const event = await firstValueFrom(callInterceptor(req));
      expect(event).toBeInstanceOf(HttpResponse);
      expect(mockLogger.debug).toHaveBeenCalledOnce();
    });

    it('should preserve the key on DELETE requests', async () => {
      const req = new HttpRequest('DELETE', '/api/v1/accounts/1', undefined, {
        headers: new HttpHeaders({ 'Idempotency-Key': 'test-key-del' }),
      });

      const event = await firstValueFrom(callInterceptor(req));
      expect(event).toBeInstanceOf(HttpResponse);
      expect(mockLogger.debug).toHaveBeenCalledOnce();
    });

    it('should preserve the key on GET requests', async () => {
      const req = new HttpRequest('GET', '/api/v1/accounts', undefined, {
        headers: new HttpHeaders({ 'Idempotency-Key': 'test-key-get' }),
      });

      const event = await firstValueFrom(callInterceptor(req));
      expect(event).toBeInstanceOf(HttpResponse);
      expect(mockLogger.debug).toHaveBeenCalledOnce();
    });
  });

  describe('when Idempotency-Key is NOT present', () => {
    it('should NOT add a key to POST requests', async () => {
      const req = new HttpRequest('POST', '/api/v1/payments', {});

      const event = await firstValueFrom(callInterceptor(req));
      expect(event).toBeInstanceOf(HttpResponse);
      expect(mockLogger.debug).not.toHaveBeenCalled();
    });

    it('should NOT add a key to GET requests', async () => {
      const req = new HttpRequest('GET', '/api/v1/accounts');

      const event = await firstValueFrom(callInterceptor(req));
      expect(event).toBeInstanceOf(HttpResponse);
      expect(mockLogger.debug).not.toHaveBeenCalled();
    });

    it('should NOT add a key to DELETE requests', async () => {
      const req = new HttpRequest('DELETE', '/api/v1/accounts/1');

      const event = await firstValueFrom(callInterceptor(req));
      expect(event).toBeInstanceOf(HttpResponse);
      expect(mockLogger.debug).not.toHaveBeenCalled();
    });

    it('should NOT add a key to PUT requests', async () => {
      const req = new HttpRequest('PUT', '/api/v1/accounts/1', {});

      const event = await firstValueFrom(callInterceptor(req));
      expect(event).toBeInstanceOf(HttpResponse);
      expect(mockLogger.debug).not.toHaveBeenCalled();
    });

    it('should NOT add a key to PATCH requests', async () => {
      const req = new HttpRequest('PATCH', '/api/v1/accounts/1', {});

      const event = await firstValueFrom(callInterceptor(req));
      expect(event).toBeInstanceOf(HttpResponse);
      expect(mockLogger.debug).not.toHaveBeenCalled();
    });
  });
});
