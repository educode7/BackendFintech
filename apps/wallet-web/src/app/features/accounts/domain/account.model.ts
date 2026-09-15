/**
 * Account domain models.
 */
export interface Account {
  accountId: string;
  userId: string;
  balance: Money;
  version: number;
  createdAt: string;
}

export interface Money {
  amount: string;
  currency: string;
}

export interface OpenAccountRequest {
  userId: string;
  initialBalance: Money;
}

export interface DepositRequest {
  amount: string;
  currency: string;
}

export interface WithdrawRequest {
  amount: string;
  currency: string;
}

export interface AccountPageResponse {
  data: Account[];
  total: number;
  page: number;
  size: number;
}
