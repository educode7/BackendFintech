/**
 * Account domain models.
 */
export interface Account {
  accountId: string;
  accountNumber: string;
  accountType: string;
  cci: string;
  iban: string;
  swiftBic: string;
  userId: string;
  holderName: string;
  holderDocumentType: string;
  holderDocumentNumber: string;
  holderEmail: string;
  holderPhone: string;
  bankCode: string;
  bankName: string;
  currency: string;
  country: string;
  balanceAmount: number;
  balanceCurrency: string;
  availableAmount: number;
  availableAmountCurrency: string;
  holdAmount: number;
  holdAmountCurrency: string;
  overdraftLimit: number;
  overdraftLimitCurrency: string;
  dailyLimit: number;
  dailyLimitCurrency: string;
  monthlyLimit: number;
  monthlyLimitCurrency: string;
  singleTransactionLimit: number;
  singleTransactionLimitCurrency: string;
  status: string;
  activatedAt: string;
  closedAt: string;
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
  accountNumber?: string;
  accountType?: string;
  cci?: string;
  iban?: string;
  swiftBic?: string;
  holderName?: string;
  holderDocumentType?: string;
  holderDocumentNumber?: string;
  holderEmail?: string;
  holderPhone?: string;
  bankCode?: string;
  bankName?: string;
  currency?: string;
  country?: string;
  dailyLimit?: string;
  monthlyLimit?: string;
  singleTransactionLimit?: string;
  overdraftLimit?: string;
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
