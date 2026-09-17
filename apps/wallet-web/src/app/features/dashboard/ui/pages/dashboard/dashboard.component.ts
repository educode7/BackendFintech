import { Component, OnInit, inject, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SlicePipe } from '@angular/common';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { AccountStore } from '@features/accounts/application/stores/account.store';
import { PaymentStore } from '@features/payments/application/stores/payment.store';
import { NotificationStore } from '@features/notifications/application/stores/notification.store';
import { AuthService } from '@core/infrastructure/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, SlicePipe, PageHeaderComponent, LoadingSpinnerComponent],
  template: `
    <app-page-header title="Dashboard" subtitle="Digital Wallet Overview" />

    @if (loading()) {
      <app-loading-spinner message="Loading dashboard data..." />
    } @else {
      <!-- Summary stats -->
      <div class="dashboard-grid">
        <div class="stat-card">
          <h3>Accounts</h3>
          <p class="stat-value">{{ accountCount() }}</p>
          <p class="stat-detail">Total balance: {{ totalBalance() }}</p>
          <a routerLink="/accounts">View all →</a>
        </div>
        <div class="stat-card">
          <h3>Payments</h3>
          <p class="stat-value">{{ paymentCount() }}</p>
          <p class="stat-detail">Total processed</p>
          <a routerLink="/payments">View all →</a>
        </div>
        <div class="stat-card">
          <h3>Notifications</h3>
          <p class="stat-value">{{ unreadCount() }}</p>
          <p class="stat-detail">Unread notifications</p>
          <a routerLink="/notifications">View all →</a>
        </div>
      </div>

      <!-- Recent payments -->
      @if (recentPayments().length > 0) {
        <section class="recent-section">
          <h2>Recent Payments</h2>
          <div class="recent-list">
            @for (payment of recentPayments(); track payment.id) {
              <div class="recent-item">
                <div class="recent-info">
                  <span class="recent-id">{{ payment.id | slice:0:8 }}...</span>
                  <span class="recent-status" [class]="payment.status.toLowerCase()">{{ payment.status }}</span>
                </div>
                <span class="recent-amount">{{ payment.amount.amount }} {{ payment.amount.currency }}</span>
              </div>
            }
          </div>
        </section>
      }

      <!-- Recent notifications -->
      @if (recentNotifications().length > 0) {
        <section class="recent-section">
          <h2>Recent Notifications</h2>
          <div class="recent-list">
            @for (notification of recentNotifications(); track notification.id) {
              <div class="recent-item">
                <div class="recent-info">
                  <span class="recent-subject">{{ notification.subject }}</span>
                  <span class="recent-status" [class]="notification.status.toLowerCase()">{{ notification.status }}</span>
                </div>
                <span class="recent-type">{{ notification.type }}</span>
              </div>
            }
          </div>
        </section>
      }
    }
  `,
  styles: [`
    .dashboard-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 1.5rem;
      margin-bottom: 2rem;
    }
    .stat-card {
      background: white;
      border: 1px solid #e5e7eb;
      border-radius: 0.75rem;
      padding: 1.5rem;
      transition: box-shadow 0.2s;
    }
    .stat-card:hover {
      box-shadow: 0 4px 12px rgba(0,0,0,0.08);
    }
    .stat-card h3 {
      margin: 0 0 0.5rem;
      color: #6b7280;
      font-size: 0.875rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    .stat-value {
      font-size: 2rem;
      font-weight: 700;
      color: #111827;
      margin: 0;
    }
    .stat-detail {
      color: #9ca3af;
      font-size: 0.875rem;
      margin: 0.25rem 0 0.75rem;
    }
    .stat-card a {
      color: #3b82f6;
      text-decoration: none;
      font-size: 0.875rem;
    }
    .stat-card a:hover {
      text-decoration: underline;
    }

    .recent-section {
      background: white;
      border: 1px solid #e5e7eb;
      border-radius: 0.75rem;
      padding: 1.5rem;
      margin-bottom: 1.5rem;
    }
    .recent-section h2 {
      margin: 0 0 1rem;
      font-size: 1.125rem;
      color: #111827;
    }
    .recent-list {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }
    .recent-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.75rem;
      background: #f9fafb;
      border-radius: 0.5rem;
    }
    .recent-info {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    .recent-id, .recent-subject {
      color: #374151;
      font-size: 0.875rem;
    }
    .recent-status {
      font-size: 0.75rem;
      padding: 0.125rem 0.5rem;
      border-radius: 1rem;
      font-weight: 500;
    }
    .recent-status.completed, .recent-status.sent {
      background: #d1fae5;
      color: #065f46;
    }
    .recent-status.pending {
      background: #fef3c7;
      color: #92400e;
    }
    .recent-status.failed {
      background: #fee2e2;
      color: #991b1b;
    }
    .recent-status.processing {
      background: #dbeafe;
      color: #1e40af;
    }
    .recent-amount {
      font-weight: 600;
      color: #111827;
    }
    .recent-type {
      font-size: 0.75rem;
      color: #6b7280;
      text-transform: uppercase;
    }
  `],
})
export class DashboardComponent implements OnInit {
  private accountStore = inject(AccountStore);
  private paymentStore = inject(PaymentStore);
  private notificationStore = inject(NotificationStore);
  private authService = inject(AuthService);

  readonly loading = computed(() =>
    this.accountStore.loading() || this.paymentStore.loading()
  );

  readonly accountCount = computed(() => this.accountStore.accounts().length);
  readonly paymentCount = computed(() => this.paymentStore.payments().length);
  readonly unreadCount = computed(() => this.notificationStore.unreadCount());

  readonly totalBalance = computed(() => {
    const accounts = this.accountStore.accounts();
    if (accounts.length === 0) return '$0.00';

    const total = accounts.reduce((sum, account) => {
      const amount = parseFloat(account.balance.amount) || 0;
      return sum + amount;
    }, 0);

    const currency = accounts[0]?.balance.currency || 'USD';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency,
    }).format(total);
  });

  readonly recentPayments = computed(() =>
    this.paymentStore.payments().slice(0, 5)
  );

  readonly recentNotifications = computed(() =>
    this.notificationStore.notifications().slice(0, 5)
  );

  ngOnInit(): void {
    this.accountStore.loadAccounts(0, 10);
    this.paymentStore.loadPayments(0, 10);

    const userInfo = this.authService.getCachedUserInfo();
    if (userInfo?.sub) {
      this.notificationStore.loadNotifications(userInfo.sub, 0, 10);
    }
  }
}
