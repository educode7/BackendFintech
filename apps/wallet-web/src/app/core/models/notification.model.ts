/**
 * Notification domain models.
 */
export interface Notification {
  id: string;
  userId: string;
  type: 'EMAIL' | 'PUSH';
  subject: string;
  body: string;
  status: 'PENDING' | 'SENT' | 'FAILED';
  createdAt: string;
  sentAt?: string;
}

export interface NotificationPageResponse {
  data: Notification[];
  total: number;
  page: number;
  size: number;
}
