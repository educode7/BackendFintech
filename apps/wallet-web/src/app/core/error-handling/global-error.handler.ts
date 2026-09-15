import { ErrorHandler, Injectable, inject } from '@angular/core';
import { LoggerService } from '@core/services/logger.service';

/**
 * Global error handler for Angular.
 * Catches all unhandled errors, logs them with context,
 * and prevents raw console.error in production.
 */
@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  private readonly logger = inject(LoggerService);

  handleError(error: unknown): void {
    const message = this.extractMessage(error);
    const stack = this.extractStack(error);

    this.logger.error(message, 'GlobalErrorHandler', {
      stack,
      errorType: error instanceof Error ? error.constructor.name : typeof error,
    });
  }

  private extractMessage(error: unknown): string {
    if (error instanceof Error) {
      return error.message;
    }
    if (typeof error === 'string') {
      return error;
    }
    return 'Unknown error occurred';
  }

  private extractStack(error: unknown): string | undefined {
    if (error instanceof Error && error.stack) {
      return error.stack;
    }
    return undefined;
  }
}
