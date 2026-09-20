import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection, ErrorHandler, APP_INITIALIZER } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideOAuthClient } from 'angular-oauth2-oidc';
import { routes } from './app.routes';
import { correlationIdInterceptor } from '@core/interceptors/correlation-id.interceptor';
import { requestIdInterceptor } from '@core/interceptors/request-id.interceptor';
import { idempotencyInterceptor } from '@core/interceptors/idempotency.interceptor';
import { traceInterceptor } from '@core/interceptors/trace.interceptor';
import { errorInterceptor } from '@core/interceptors/error.interceptor';
import { tokenRefreshInterceptor } from '@core/interceptors/token-refresh.interceptor';
import { GlobalErrorHandler } from '@core/error-handling/global-error.handler';
import { AuthService } from '@core/infrastructure/auth.service';

function initializeAuth(authService: AuthService): () => Promise<void> {
  return () => authService.init();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideOAuthClient({
      resourceServer: {
        sendAccessToken: true,
      },
    }),
    { provide: ErrorHandler, useClass: GlobalErrorHandler },
    {
      provide: APP_INITIALIZER,
      useFactory: initializeAuth,
      deps: [AuthService],
      multi: true,
    },
    provideHttpClient(
      withInterceptors([
        correlationIdInterceptor,
        requestIdInterceptor,
        idempotencyInterceptor,
        traceInterceptor,
        tokenRefreshInterceptor,
        errorInterceptor,
      ])
    ),
  ],
};
