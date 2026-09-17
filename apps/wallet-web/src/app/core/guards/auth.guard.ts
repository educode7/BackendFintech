import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, catchError, of } from 'rxjs';
import { AuthService } from '@core/infrastructure/auth.service';

/**
 * Auth guard — redirects to /login if not authenticated.
 *
 * Checks the user's identity via GET /auth/me (backend validates the access token).
 * Falls back to local JWT expiry check for fast path.
 */
export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const authService = inject(AuthService);

  // Fast path: cached identity is still valid
  if (authService.isAuthenticated()) {
    return true;
  }

  // Slow path: fetch identity from backend (validates access token)
  return authService.getUserInfo().pipe(
    map(() => true),
    catchError(() => {
      router.navigate(['/login']);
      return of(false);
    })
  );
};
