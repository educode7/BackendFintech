import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@core/infrastructure/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  template: `
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <div class="logo">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
              <rect width="48" height="48" rx="12" fill="#3b82f6"/>
              <path d="M24 12C17.373 12 12 17.373 12 24s5.373 12 12 12 12-5.373 12-12S30.627 12 24 12zm0 3.6c2 0 3.6 1.6 3.6 3.6s-1.6 3.6-3.6 3.6-3.6-1.6-3.6-3.6 1.6-3.6 3.6-3.6zm0 19.2c-3 0-5.64-1.56-7.2-3.96.06-2.46 4.8-3.84 7.2-3.84s7.14 1.38 7.2 3.84c-1.56 2.4-4.2 3.96-7.2 3.96z" fill="white"/>
            </svg>
          </div>
          <h1>Wallet</h1>
          <p class="subtitle">Digital Wallet Platform</p>
        </div>

        <div class="login-body">
          <p class="description">
            Access your accounts, make payments, and manage your finances.
          </p>

          <button class="login-button" (click)="login()" [disabled]="loading">
            @if (loading) {
              <span class="spinner"></span>
              Connecting...
            } @else {
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none" class="key-icon">
                <path d="M10 2C6.686 2 4 4.686 4 8c0 1.5.5 2.87 1.34 3.97L4 16l4.03-1.34C9.13 15.5 10.5 16 12 16c3.314 0 6-2.686 6-6s-2.686-6-6-6zm0 2.4c1.988 0 3.6 1.612 3.6 3.6S11.988 11.6 10 11.6 6.4 9.988 6.4 8 8.012 4.4 10 4.4z" fill="currentColor"/>
              </svg>
              Iniciar sesión
            }
          </button>

          <p class="hint">
            You'll be redirected to Keycloak to authenticate
          </p>
        </div>

        <div class="login-footer">
          <p>Secured by Keycloak OIDC</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      padding: 1rem;
    }

    .login-card {
      background: white;
      border-radius: 1rem;
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
      width: 100%;
      max-width: 400px;
      overflow: hidden;
    }

    .login-header {
      text-align: center;
      padding: 2.5rem 2rem 1.5rem;
      background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
      color: white;
    }

    .logo {
      margin-bottom: 1rem;
    }

    .logo svg {
      display: inline-block;
    }

    .login-header h1 {
      margin: 0;
      font-size: 1.75rem;
      font-weight: 700;
      letter-spacing: -0.025em;
    }

    .subtitle {
      margin: 0.25rem 0 0;
      opacity: 0.9;
      font-size: 0.9rem;
    }

    .login-body {
      padding: 2rem;
      text-align: center;
    }

    .description {
      color: #6b7280;
      font-size: 0.95rem;
      line-height: 1.5;
      margin: 0 0 1.5rem;
    }

    .login-button {
      width: 100%;
      padding: 0.875rem 1.5rem;
      font-size: 1rem;
      font-weight: 600;
      color: white;
      background: #3b82f6;
      border: none;
      border-radius: 0.5rem;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      transition: all 0.2s;
    }

    .login-button:hover:not(:disabled) {
      background: #2563eb;
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
    }

    .login-button:active:not(:disabled) {
      transform: translateY(0);
    }

    .login-button:disabled {
      opacity: 0.7;
      cursor: not-allowed;
    }

    .key-icon {
      flex-shrink: 0;
    }

    .spinner {
      width: 18px;
      height: 18px;
      border: 2px solid rgba(255, 255, 255, 0.3);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 0.6s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .hint {
      color: #9ca3af;
      font-size: 0.8rem;
      margin: 1rem 0 0;
    }

    .login-footer {
      padding: 1rem 2rem;
      background: #f9fafb;
      text-align: center;
      border-top: 1px solid #e5e7eb;
    }

    .login-footer p {
      margin: 0;
      color: #9ca3af;
      font-size: 0.75rem;
    }
  `],
})
export class LoginComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  loading = false;

  ngOnInit() {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }
  }

  login(): void {
    this.loading = true;
    this.authService.login();
  }
}
