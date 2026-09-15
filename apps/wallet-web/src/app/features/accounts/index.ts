// Accounts feature barrel export
export { AccountListComponent } from './ui/pages/account-list/account-list.component';
export { AccountDetailComponent } from './ui/pages/account-detail/account-detail.component';
export { AccountDepositComponent } from './ui/pages/account-deposit/account-deposit.component';
export type { Account, Money, OpenAccountRequest, DepositRequest, WithdrawRequest } from './domain/account.model';
export { AccountAdapter } from './infrastructure/account.adapter';
export { AccountStore } from './application/stores/account.store';
