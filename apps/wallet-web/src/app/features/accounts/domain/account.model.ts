/**
 * Account domain models.
 */
export interface Account {
  accountId: string;
  userId: string;
  balanceAmount: number;
  balanceCurrency: string;
  status: string;
  version: number;
  lastUpdated: string;
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
  items: Account[];
  total: number;
  page: number;
  size: number;
}
