import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '@core/infrastructure/auth.service';

/**
 * Auth guard — redirects to login page if not authenticated.
 * The login page handles the Keycloak redirect.
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  // Redirect to login page (root route)
  return router.createUrlTree(['/']);
};

/**
 * Login guard — redirects to dashboard if already authenticated.
 * Prevents showing the login page to users with an active session.
 */
export const loginGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return router.createUrlTree(['/dashboard']);
  }

  return true;
};
