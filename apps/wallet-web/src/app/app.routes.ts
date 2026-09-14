import { Routes } from '@angular/router';
import { authGuard } from '@core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@features/dashboard/pages/dashboard/dashboard.component').then(
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
          import('@features/payments/pages/payment-list/payment-list.component').then(
            (m) => m.PaymentListComponent
          ),
      },
      {
        path: ':id',
        loadComponent: () =>
          import('@features/payments/pages/payment-detail/payment-detail.component').then(
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
          import('@features/accounts/pages/account-list/account-list.component').then(
            (m) => m.AccountListComponent
          ),
      },
      {
        path: ':accountId',
        loadComponent: () =>
          import('@features/accounts/pages/account-detail/account-detail.component').then(
            (m) => m.AccountDetailComponent
          ),
      },
      {
        path: ':accountId/deposit',
        loadComponent: () =>
          import('@features/accounts/pages/account-deposit/account-deposit.component').then(
            (m) => m.AccountDepositComponent
          ),
      },
    ],
  },
  {
    path: 'notifications',
    canActivate: [authGuard],
    loadComponent: () =>
      import('@features/notifications/pages/notification-list/notification-list.component').then(
        (m) => m.NotificationListComponent
      ),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
