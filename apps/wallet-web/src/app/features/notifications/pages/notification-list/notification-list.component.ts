import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NotificationService } from '@services/notification.service';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { ErrorDisplayComponent } from '@shared/components/error-display/error-display.component';
import { RelativeTimePipe } from '@shared/pipes/relative-time.pipe';
import type { Notification } from '@models/notification.model';

@Component({
  selector: 'app-notification-list',
  standalone: true,
  imports: [PageHeaderComponent, LoadingSpinnerComponent, ErrorDisplayComponent, RelativeTimePipe],
  template: `
    <app-page-header title="Notifications" subtitle="Your notification history" />

    @if (loading()) {
      <app-loading-spinner message="Loading notifications..." />
    } @else if (error()) {
      <app-error-display [detail]="error()!" />
    } @else {
      <div class="notification-list">
        @for (n of notifications(); track n.id) {
          <div class="notification-card" [class.unread]="n.status === 'PENDING'">
            <div class="notification-header">
              <span class="type-badge" [class]="n.type.toLowerCase()">{{ n.type }}</span>
              <span class="time">{{ n.createdAt | relativeTime }}</span>
            </div>
            <h4>{{ n.subject }}</h4>
            <p>{{ n.body }}</p>
            <span class="status" [class]="n.status.toLowerCase()">{{ n.status }}</span>
          </div>
        } @empty {
          <p class="empty">No notifications</p>
        }
      </div>
    }
  `,
  styles: [`
    .notification-list { display: flex; flex-direction: column; gap: 0.75rem; }
    .notification-card {
      background: white; border: 1px solid #e5e7eb; border-radius: 0.5rem;
      padding: 1rem; transition: border-color 0.2s;
    }
    .notification-card.unread { border-left: 3px solid #3b82f6; }
    .notification-header { display: flex; justify-content: space-between; margin-bottom: 0.5rem; }
    .type-badge { font-size: 0.7rem; padding: 0.15rem 0.4rem; border-radius: 9999px; font-weight: 500; }
    .type-badge.email { background: #dbeafe; color: #1e40af; }
    .type-badge.push { background: #dcfce7; color: #166534; }
    .time { color: #9ca3af; font-size: 0.8rem; }
    h4 { margin: 0 0 0.25rem; font-weight: 600; }
    p { margin: 0; color: #4b5563; font-size: 0.875rem; }
    .status { font-size: 0.75rem; font-weight: 500; }
    .status.sent { color: #16a34a; }
    .status.pending { color: #d97706; }
    .status.failed { color: #dc2626; }
    .empty { text-align: center; color: #999; padding: 2rem; }
  `],
})
export class NotificationListComponent {
  private readonly notificationService = inject(NotificationService);

  loading = signal(true);
  error = signal<string | null>(null);
  notifications = signal<Notification[]>([]);

  constructor() {
    // TODO: get userId from auth context
    this.notificationService.listByUser('current-user').subscribe({
      next: (res) => { this.notifications.set(res.data); this.loading.set(false); },
      error: (err) => { this.error.set(err.detail); this.loading.set(false); },
    });
  }
}
