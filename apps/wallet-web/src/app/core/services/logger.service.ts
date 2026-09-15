import { Injectable } from '@angular/core';

export type LogLevel = 'debug' | 'info' | 'warn' | 'error';

export interface LogEntry {
  timestamp: string;
  level: LogLevel;
  message: string;
  context?: string;
  traceId?: string;
  requestId?: string;
  [key: string]: unknown;
}

const LOG_LEVELS: Record<LogLevel, number> = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3,
};

/**
 * Structured logger service.
 * Wraps console methods with structured JSON output.
 * Debug logs are disabled in production.
 */
@Injectable({ providedIn: 'root' })
export class LoggerService {
  private readonly minLevel: LogLevel = this.isProduction() ? 'info' : 'debug';

  debug(message: string, context?: string, extra?: Record<string, unknown>): void {
    this.log('debug', message, context, extra);
  }

  info(message: string, context?: string, extra?: Record<string, unknown>): void {
    this.log('info', message, context, extra);
  }

  warn(message: string, context?: string, extra?: Record<string, unknown>): void {
    this.log('warn', message, context, extra);
  }

  error(message: string, context?: string, extra?: Record<string, unknown>): void {
    this.log('error', message, context, extra);
  }

  private log(level: LogLevel, message: string, context?: string, extra?: Record<string, unknown>): void {
    if (LOG_LEVELS[level] < LOG_LEVELS[this.minLevel]) {
      return;
    }

    const entry: LogEntry = {
      timestamp: new Date().toISOString(),
      level,
      message: this.sanitize(message),
      context,
      ...this.sanitizeExtra(extra),
    };

    switch (level) {
      case 'debug':
        console.debug(JSON.stringify(entry));
        break;
      case 'info':
        console.info(JSON.stringify(entry));
        break;
      case 'warn':
        console.warn(JSON.stringify(entry));
        break;
      case 'error':
        console.error(JSON.stringify(entry));
        break;
    }
  }

  /**
   * Sanitize message to strip PII patterns.
   */
  private sanitize(text: string): string {
    return text
      .replace(/\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b/g, '[REDACTED_EMAIL]')
      .replace(/\b\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4}\b/g, '[REDACTED_CARD]')
      .replace(/bearer\s+[A-Za-z0-9\-._~+\/]+=*/gi, 'bearer [REDACTED]');
  }

  private sanitizeExtra(extra?: Record<string, unknown>): Record<string, unknown> {
    if (!extra) return {};
    const sanitized: Record<string, unknown> = {};
    const sensitiveKeys = new Set(['password', 'token', 'secret', 'authorization', 'cookie']);

    for (const [key, value] of Object.entries(extra)) {
      if (sensitiveKeys.has(key.toLowerCase())) {
        sanitized[key] = '[REDACTED]';
      } else {
        sanitized[key] = value;
      }
    }
    return sanitized;
  }

  private isProduction(): boolean {
    try {
      return typeof window !== 'undefined' &&
        (window as unknown as Record<string, unknown>)['__env'] === 'production';
    } catch {
      return false;
    }
  }
}
