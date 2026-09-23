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
  readAt?: string | null;
}

export interface NotificationPageResponse {
  items: Notification[];
  total: number;
  page: number;
  size: number;
}
