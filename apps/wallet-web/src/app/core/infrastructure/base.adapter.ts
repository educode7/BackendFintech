import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { environment } from '@env/environment';
import type { ProblemDetail } from '@shared/domain/error.model';

/**
 * Base HTTP adapter with error handling and base URL configuration.
 */
@Injectable({ providedIn: 'root' })
export class BaseAdapter {
  protected readonly http = inject(HttpClient);
  protected readonly baseUrl = environment.apiGateway;

  protected handleError(error: HttpErrorResponse): Observable<never> {
    const problem: ProblemDetail = error.error?.type
      ? error.error
      : {
          type: 'about:blank',
          title: error.statusText,
          status: error.status,
          detail: error.message,
        };
    return throwError(() => problem);
  }
}
