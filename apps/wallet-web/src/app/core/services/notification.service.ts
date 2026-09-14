import { inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { BaseService } from './base.service';
import type { Notification, NotificationPageResponse } from '@models/notification.model';

/**
 * Notification API service — consumes /api/v1/notifications.
 */
export class NotificationService extends BaseService {
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
