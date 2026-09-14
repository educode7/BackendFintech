import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="app-shell">
      <nav class="sidebar">
        <div class="logo">
          <h2>💰 Wallet</h2>
        </div>
        <ul class="nav-list">
          <li>
            <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">
              Dashboard
            </a>
          </li>
          <li>
            <a routerLink="/payments" routerLinkActive="active">Payments</a>
          </li>
          <li>
            <a routerLink="/accounts" routerLinkActive="active">Accounts</a>
          </li>
          <li>
            <a routerLink="/notifications" routerLinkActive="active">Notifications</a>
          </li>
        </ul>
      </nav>
      <main class="content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [`
    .app-shell { display: flex; min-height: 100vh; }
    .sidebar {
      width: 240px; background: #1a1a2e; color: white;
      display: flex; flex-direction: column; flex-shrink: 0;
    }
    .logo { padding: 1.5rem; border-bottom: 1px solid #2d2d44; }
    .logo h2 { margin: 0; font-size: 1.25rem; }
    .nav-list { list-style: none; padding: 0.5rem 0; margin: 0; }
    .nav-list a {
      display: block; padding: 0.75rem 1.5rem; color: #a0a0b8;
      text-decoration: none; transition: all 0.2s;
    }
    .nav-list a:hover { background: #2d2d44; color: white; }
    .nav-list a.active { background: #3b82f6; color: white; font-weight: 500; }
    .content { flex: 1; padding: 2rem; background: #f5f5f7; overflow-y: auto; }
  `],
})
export class App {}
