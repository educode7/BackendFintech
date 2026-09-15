import { TestBed } from '@angular/core/testing';
import { GlobalErrorHandler } from './global-error.handler';
import { LoggerService } from '@core/infrastructure/logger.service';

describe('GlobalErrorHandler', () => {
  let handler: GlobalErrorHandler;
  let loggerSpy: { error: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    loggerSpy = { error: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        GlobalErrorHandler,
        { provide: LoggerService, useValue: loggerSpy },
      ],
    });

    handler = TestBed.inject(GlobalErrorHandler);
  });

  it('should be created', () => {
    expect(handler).toBeTruthy();
  });

  it('should log Error instances with message and stack', () => {
    const error = new Error('Something failed');
    handler.handleError(error);

    expect(loggerSpy.error).toHaveBeenCalledOnce();
    const [message, context, extra] = loggerSpy.error.mock.calls[0];
    expect(message).toBe('Something failed');
    expect(context).toBe('GlobalErrorHandler');
    expect(extra.errorType).toBe('Error');
    expect(extra.stack).toContain('Something failed');
  });

  it('should log string errors', () => {
    handler.handleError('string error');

    const [message] = loggerSpy.error.mock.calls[0];
    expect(message).toBe('string error');
  });

  it('should log unknown errors with fallback message', () => {
    handler.handleError(null);

    const [message] = loggerSpy.error.mock.calls[0];
    expect(message).toBe('Unknown error occurred');
  });
});
