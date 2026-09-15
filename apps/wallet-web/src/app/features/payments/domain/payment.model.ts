/**
 * Payment domain models.
 */
export interface Payment {
  id: string;
  userId: string;
  amount: Money;
  status: PaymentStatus;
  idempotencyKey: string;
  createdAt: string;
  completedAt?: string;
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
  data: Payment[];
  total: number;
  page: number;
  size: number;
}
