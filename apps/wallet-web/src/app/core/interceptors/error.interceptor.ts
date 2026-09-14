import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { inject } from '@angular/core';
import { Router } from '@angular/router';

/**
 * Global error interceptor.
 * Handles 401 → redirect to login, 429 → retry hint, generic → toast.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      switch (error.status) {
        case 401:
          // Token expired or invalid — redirect to login
          router.navigate(['/login']);
          break;
        case 429:
          console.warn('Rate limited. Retry after:', error.headers.get('Retry-After'));
          break;
        case 0:
          console.error('Network error — backend unreachable');
          break;
      }
      return throwError(() => error);
    })
  );
};
