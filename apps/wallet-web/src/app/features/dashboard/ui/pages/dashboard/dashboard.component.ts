import { Component, signal } from '@angular/core';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [PageHeaderComponent],
  template: `
    <app-page-header title="Dashboard" subtitle="Digital Wallet Overview" />

    <div class="dashboard-grid">
      <div class="stat-card">
        <h3>Payments</h3>
        <p class="stat-value">—</p>
        <a routerLink="/payments">View all →</a>
      </div>
      <div class="stat-card">
        <h3>Accounts</h3>
        <p class="stat-value">—</p>
        <a routerLink="/accounts">View all →</a>
      </div>
      <div class="stat-card">
        <h3>Notifications</h3>
        <p class="stat-value">—</p>
        <a routerLink="/notifications">View all →</a>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 1.5rem; }
    .stat-card {
      background: white; border: 1px solid #e5e7eb; border-radius: 0.75rem;
      padding: 1.5rem; transition: box-shadow 0.2s;
    }
    .stat-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
    .stat-card h3 { margin: 0 0 0.5rem; color: #6b7280; font-size: 0.875rem; text-transform: uppercase; letter-spacing: 0.05em; }
    .stat-value { font-size: 2rem; font-weight: 700; color: #111827; margin: 0; }
    .stat-card a { color: #3b82f6; text-decoration: none; font-size: 0.875rem; }
    .stat-card a:hover { text-decoration: underline; }
  `],
})
export class DashboardComponent {}
