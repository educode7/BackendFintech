import { ApplicationConfig, provideBrowserGlobalErrorListeners, ErrorHandler } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { correlationIdInterceptor } from '@core/interceptors/correlation-id.interceptor';
import { requestIdInterceptor } from '@core/interceptors/request-id.interceptor';
import { idempotencyInterceptor } from '@core/interceptors/idempotency.interceptor';
import { traceInterceptor } from '@core/interceptors/trace.interceptor';
import { errorInterceptor } from '@core/interceptors/error.interceptor';
import { GlobalErrorHandler } from '@core/error-handling/global-error.handler';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    { provide: ErrorHandler, useClass: GlobalErrorHandler },
    provideHttpClient(
      withInterceptors([
        correlationIdInterceptor,
        requestIdInterceptor,
        idempotencyInterceptor,
        traceInterceptor,
        errorInterceptor,
      ])
    ),
  ],
};
