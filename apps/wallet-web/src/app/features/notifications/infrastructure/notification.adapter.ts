import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { BaseAdapter } from '@core/infrastructure/base.adapter';
import type { Notification, NotificationPageResponse } from '../domain/notification.model';

/**
 * Notification HTTP adapter — consumes /api/v1/notifications.
 */
@Injectable({ providedIn: 'root' })
export class NotificationAdapter extends BaseAdapter {
  private readonly notificationHttp = inject(HttpClient);

  listByUser(userId: string, page = 0, size = 20): Observable<NotificationPageResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.notificationHttp.get<NotificationPageResponse>(
      `${this.baseUrl}/api/v1/notifications/${userId}`,
      { params }
    ).pipe(catchError(this.handleError));
  }
}
