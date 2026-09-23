import { Component, inject, OnInit } from '@angular/core';
import { NotificationStore } from '../../../application/stores/notification.store';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { ErrorDisplayComponent } from '@shared/components/error-display/error-display.component';
import { RelativeTimePipe } from '@shared/pipes/relative-time.pipe';
import { AuthService } from '@core/infrastructure/auth.service';

@Component({
  selector: 'app-notification-list',
  standalone: true,
  imports: [PageHeaderComponent, LoadingSpinnerComponent, ErrorDisplayComponent, RelativeTimePipe],
  template: `
    <app-page-header title="Notifications" subtitle="Your notification history" />

    @if (store.loading()) {
      <app-loading-spinner message="Loading notifications..." />
    } @else if (store.error()) {
      <app-error-display [detail]="store.error()!" />
    } @else {
      @if (store.unreadCount() > 0) {
        <div class="list-toolbar">
          <button type="button" class="mark-all-btn" (click)="markAllAsRead()">Mark all as read</button>
        </div>
      }
      <div class="notification-list">
        @for (n of store.notifications(); track n.id) {
          <div class="notification-card" [class.unread]="!n.readAt" (click)="store.markAsRead(n.id)">
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
    .list-toolbar { display: flex; justify-content: flex-end; margin-bottom: 0.5rem; }
    .mark-all-btn {
      font-size: 0.85rem; padding: 0.35rem 0.75rem; border: 1px solid #3b82f6;
      background: white; color: #3b82f6; border-radius: 0.375rem; cursor: pointer;
    }
    .mark-all-btn:hover { background: #eff6ff; }
    .notification-list { display: flex; flex-direction: column; gap: 0.75rem; }
    .notification-card {
      background: white; border: 1px solid #e5e7eb; border-radius: 0.5rem;
      padding: 1rem; transition: border-color 0.2s; cursor: pointer;
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
export class NotificationListComponent implements OnInit {
  readonly store = inject(NotificationStore);
  private readonly authService = inject(AuthService);

  ngOnInit() {
    const userId = this.authService.getUserInfo()?.sub;
    if (userId) {
      this.store.loadNotifications(userId);
    }
  }

  markAllAsRead(): void {
    const userId = this.authService.getUserInfo()?.sub;
    if (userId) {
      this.store.markAllAsRead(userId);
    }
  }
}
