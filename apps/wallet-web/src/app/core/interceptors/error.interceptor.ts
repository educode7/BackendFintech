import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { LoggerService } from '@core/services/logger.service';

/**
 * Global error interceptor.
 * Handles 401 → redirect to login, 429 → retry hint, 0 → network error.
 * Uses structured logging instead of console.*.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const logger = inject(LoggerService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      switch (error.status) {
        case 401:
          router.navigate(['/login']);
          break;
        case 429:
          logger.warn('Rate limited', 'ErrorInterceptor', {
            retryAfter: error.headers.get('Retry-After'),
            url: req.url,
          });
          break;
        case 0:
          logger.error('Network error — backend unreachable', 'ErrorInterceptor', {
            url: req.url,
          });
          break;
        default:
          logger.error('HTTP error', 'ErrorInterceptor', {
            status: error.status,
            url: req.url,
          });
      }
      return throwError(() => error);
    })
  );
};
