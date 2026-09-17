import { Routes } from '@angular/router';
import { authGuard } from '@core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@features/dashboard/ui/pages/dashboard/dashboard.component').then(
        (m) => m.DashboardComponent
      ),
  },
  {
    path: 'payments',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('@features/payments/ui/pages/payment-list/payment-list.component').then(
            (m) => m.PaymentListComponent
          ),
      },
      {
        path: ':id',
        loadComponent: () =>
          import('@features/payments/ui/pages/payment-detail/payment-detail.component').then(
            (m) => m.PaymentDetailComponent
          ),
      },
    ],
  },
  {
    path: 'accounts',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('@features/accounts/ui/pages/account-list/account-list.component').then(
            (m) => m.AccountListComponent
          ),
      },
      {
        path: ':accountId',
        loadComponent: () =>
          import('@features/accounts/ui/pages/account-detail/account-detail.component').then(
            (m) => m.AccountDetailComponent
          ),
      },
      {
        path: ':accountId/deposit',
        loadComponent: () =>
          import('@features/accounts/ui/pages/account-deposit/account-deposit.component').then(
            (m) => m.AccountDepositComponent
          ),
      },
    ],
  },
  {
    path: 'notifications',
    canActivate: [authGuard],
    loadComponent: () =>
      import('@features/notifications/ui/pages/notification-list/notification-list.component').then(
        (m) => m.NotificationListComponent
      ),
  },
  {
    path: 'mfa',
    canActivate: [authGuard],
    children: [
      {
        path: 'setup',
        loadComponent: () =>
          import('@features/auth/ui/pages/mfa-setup/mfa-setup.component').then(
            (m) => m.MfaSetupComponent
          ),
      },
      {
        path: 'verify',
        loadComponent: () =>
          import('@features/auth/ui/pages/mfa-login/mfa-login.component').then(
            (m) => m.MfaLoginComponent
          ),
      },
      {
        path: 'disable',
        loadComponent: () =>
          import('@features/auth/ui/pages/mfa-disable/mfa-disable.component').then(
            (m) => m.MfaDisableComponent
          ),
      },
    ],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
