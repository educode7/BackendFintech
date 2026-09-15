import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '@core/infrastructure/auth.service';

/**
 * Auth guard — redirects to /login if not authenticated.
 * Uses in-memory token storage (AuthService), NOT localStorage.
 */
export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const authService = inject(AuthService);

  if (!authService.isAuthenticated()) {
    router.navigate(['/login']);
    return false;
  }

  return true;
};
