import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '@core/infrastructure/auth.service';

/**
 * Auth guard — redirects to Keycloak login if not authenticated.
 *
 * Uses angular-oauth2-oidc's hasValidAccessToken() for fast local check.
 * The token was validated by Keycloak during the Authorization Code + PKCE flow.
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  // Redirect to Keycloak login
  authService.login();
  return false;
};
