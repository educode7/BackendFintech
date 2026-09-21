import { Component, inject, computed } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '@core/infrastructure/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    @if (isAuthenticated()) {
      <div class="app-shell">
        <a href="#main-content" class="skip-link">Skip to main content</a>
        <nav class="sidebar">
          <div class="logo">
            <h2>💰 Wallet</h2>
          </div>
          <ul class="nav-list">
            <li>
              <a routerLink="/dashboard" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">
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
          <div class="sidebar-footer">
            <button class="logout-btn" (click)="logout()">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M2 2.667A1.333 1.333 0 0 1 3.333 1.333h2.667A1.333 1.333 0 0 1 7.333 2.667v10.666a1.333 1.333 0 0 1-1.333 1.333H3.333A1.333 1.333 0 0 1 2 13.333V2.667Z" stroke="currentColor" stroke-width="1.5"/>
                <path d="M10 5.333l3.334 2.667-3.334 2.667M13.333 8H6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              Cerrar sesión
            </button>
          </div>
        </nav>
        <main id="main-content" class="content" tabindex="-1">
          <router-outlet />
        </main>
      </div>
    } @else {
      <router-outlet />
    }
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
    .sidebar-footer {
      margin-top: auto;
      padding: 1rem 1.5rem;
      border-top: 1px solid #2d2d44;
    }
    .logout-btn {
      display: flex; align-items: center; gap: 0.5rem;
      width: 100%; padding: 0.625rem 0.75rem;
      background: none; border: 1px solid #4a4a6a; border-radius: 0.5rem;
      color: #a0a0b8; font-size: 0.875rem; cursor: pointer;
      transition: all 0.2s;
    }
    .logout-btn:hover { background: #2d2d44; color: #ef4444; border-color: #ef4444; }
    .content { flex: 1; padding: 2rem; background: #f5f5f7; overflow-y: auto; }
    .skip-link {
      position: absolute;
      top: -40px;
      left: 0;
      background: #3b82f6;
      color: white;
      padding: 0.5rem 1rem;
      z-index: 100;
      transition: top 0.2s;
    }
    .skip-link:focus {
      top: 0;
    }
  `],
})
export class App {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  readonly isAuthenticated = computed(() => this.authService.isAuthenticated());

  logout(): void {
    this.authService.logout();
  }

  constructor() {
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(() => {
        const mainContent = document.getElementById('main-content');
        if (mainContent) {
          mainContent.focus();
        }
      });
  }
}
