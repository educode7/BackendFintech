/**
 * Payment domain models.
 */
export interface Payment {
  id: string;
  accountId: string;
  userId: string;
  amount: number;
  currency: string;
  status: PaymentStatus;
  createdAt: string;
  updatedAt: string;
}

export interface Money {
  amount: string;
  currency: string;
}

export type PaymentStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface ProcessPaymentRequest {
  userId: string;
  amount: Money;
}

export interface PaymentPageResponse {
  items: Payment[];
  total: number;
  page: number;
  size: number;
}
