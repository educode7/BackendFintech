import { TestBed } from '@angular/core/testing';
import { LoggerService, LogLevel } from './logger.service';

describe('LoggerService', () => {
  let service: LoggerService;
  let consoleSpies: Record<LogLevel, ReturnType<typeof vi.spyOn>>;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LoggerService);

    consoleSpies = {
      debug: vi.spyOn(console, 'debug').mockImplementation(() => {}),
      info: vi.spyOn(console, 'info').mockImplementation(() => {}),
      warn: vi.spyOn(console, 'warn').mockImplementation(() => {}),
      error: vi.spyOn(console, 'error').mockImplementation(() => {}),
    };
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should log info with structured JSON', () => {
    service.info('User logged in', 'AuthService');

    expect(consoleSpies.info).toHaveBeenCalledOnce();
    const logged = JSON.parse(consoleSpies.info.mock.calls[0][0]);
    expect(logged.level).toBe('info');
    expect(logged.message).toBe('User logged in');
    expect(logged.context).toBe('AuthService');
    expect(logged.timestamp).toBeDefined();
  });

  it('should log warn with structured JSON', () => {
    service.warn('Rate limited', 'Http');

    expect(consoleSpies.warn).toHaveBeenCalledOnce();
    const logged = JSON.parse(consoleSpies.warn.mock.calls[0][0]);
    expect(logged.level).toBe('warn');
    expect(logged.message).toBe('Rate limited');
  });

  it('should log error with structured JSON', () => {
    service.error('Request failed', 'Http', { url: '/api/v1/payments' });

    expect(consoleSpies.error).toHaveBeenCalledOnce();
    const logged = JSON.parse(consoleSpies.error.mock.calls[0][0]);
    expect(logged.level).toBe('error');
    expect(logged.url).toBe('/api/v1/payments');
  });

  it('should redact email addresses from messages', () => {
    service.info('Email sent to user@example.com');

    const logged = JSON.parse(consoleSpies.info.mock.calls[0][0]);
    expect(logged.message).toContain('[REDACTED_EMAIL]');
    expect(logged.message).not.toContain('user@example.com');
  });

  it('should redact credit card numbers from messages', () => {
    service.info('Card 4111 1111 1111 1111 processed');

    const logged = JSON.parse(consoleSpies.info.mock.calls[0][0]);
    expect(logged.message).toContain('[REDACTED_CARD]');
  });

  it('should redact bearer tokens from messages', () => {
    service.info('Token: Bearer eyJhbGciOiJIUzI1NiJ9.test.signature');

    const logged = JSON.parse(consoleSpies.info.mock.calls[0][0]);
    expect(logged.message).toContain('bearer [REDACTED]');
  });

  it('should redact sensitive extra fields', () => {
    service.info('Auth', 'Auth', { password: 'secret123', token: 'abc' });

    const logged = JSON.parse(consoleSpies.info.mock.calls[0][0]);
    expect(logged.password).toBe('[REDACTED]');
    expect(logged.token).toBe('[REDACTED]');
  });

  it('should preserve non-sensitive extra fields', () => {
    service.info('Request', 'Http', { method: 'GET', status: 200 });

    const logged = JSON.parse(consoleSpies.info.mock.calls[0][0]);
    expect(logged.method).toBe('GET');
    expect(logged.status).toBe(200);
  });
});
