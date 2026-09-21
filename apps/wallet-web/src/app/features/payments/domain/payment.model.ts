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
  paymentType: string;
  beneficiaryName: string;
  beneficiaryDocumentType: string;
  beneficiaryDocumentNumber: string;
  beneficiaryAccountNumber: string;
  beneficiaryBankCode: string;
  beneficiaryBankName: string;
  senderName: string;
  senderDocumentType: string;
  senderDocumentNumber: string;
  reference: string;
  externalReference: string;
  channel: string;
  processedAt: string;
  failedAt: string;
  failureReason: string;
  retryCount: number;
  feeAmount: number;
  feeCurrency: string;
  createdAt: string;
  updatedAt: string;
}

export interface Money {
  amount: string;
  currency: string;
}

export type PaymentStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface ProcessPaymentRequest {
  accountId: string;
  userId: string;
  amount: Money;
  paymentType?: string;
  beneficiaryName?: string;
  beneficiaryDocumentType?: string;
  beneficiaryDocumentNumber?: string;
  beneficiaryAccountNumber?: string;
  beneficiaryBankCode?: string;
  beneficiaryBankName?: string;
  reference?: string;
  channel?: string;
}

export interface PaymentPageResponse {
  items: Payment[];
  total: number;
  page: number;
  size: number;
}
