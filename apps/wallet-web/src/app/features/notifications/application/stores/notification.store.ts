import { Injectable, signal, computed } from '@angular/core';
import { NotificationAdapter } from '../../infrastructure/notification.adapter';
import type { Notification } from '../../domain/notification.model';

/**
 * Notification store — manages notification state with signals.
 */
@Injectable({ providedIn: 'root' })
export class NotificationStore {
  private readonly adapter: NotificationAdapter;

  private readonly _notifications = signal<Notification[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _total = signal(0);

  readonly notifications = this._notifications.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly total = this._total.asReadonly();
  readonly hasNotifications = computed(() => this._notifications().length > 0);
  readonly unreadCount = computed(() =>
    this._notifications().filter((n) => n.status === 'PENDING').length
  );

  constructor(adapter: NotificationAdapter) {
    this.adapter = adapter;
  }

  loadNotifications(userId: string, page = 0, size = 20): void {
    this._loading.set(true);
    this._error.set(null);
    this.adapter.listByUser(userId, page, size).subscribe({
      next: (res) => {
        this._notifications.set(res.data);
        this._total.set(res.total);
        this._loading.set(false);
      },
      error: (err) => {
        this._error.set(err.detail || 'Failed to load notifications');
        this._loading.set(false);
      },
    });
  }

  clearError(): void {
    this._error.set(null);
  }

  reset(): void {
    this._notifications.set([]);
    this._loading.set(false);
    this._error.set(null);
    this._total.set(0);
  }
}
