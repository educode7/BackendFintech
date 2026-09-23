import { TestBed } from '@angular/core/testing';
import { NotificationStore } from './notification.store';
import { NotificationAdapter } from '../../infrastructure/notification.adapter';
import { of, throwError } from 'rxjs';

describe('NotificationStore', () => {
  let store: NotificationStore;
  let adapterSpy: {
    listByUser: ReturnType<typeof vi.fn>;
    markRead: ReturnType<typeof vi.fn>;
    markAllRead: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    adapterSpy = { listByUser: vi.fn(), markRead: vi.fn(), markAllRead: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        NotificationStore,
        { provide: NotificationAdapter, useValue: adapterSpy },
      ],
    });

    store = TestBed.inject(NotificationStore);
  });

  it('should be created', () => {
    expect(store).toBeTruthy();
  });

  it('should start with empty state', () => {
    expect(store.notifications()).toEqual([]);
    expect(store.loading()).toBe(false);
    expect(store.error()).toBeNull();
    expect(store.total()).toBe(0);
    expect(store.hasNotifications()).toBe(false);
    expect(store.unreadCount()).toBe(0);
  });

  describe('loadNotifications', () => {
    it('should load notifications', () => {
      const mockNotifications = [
        { id: 'n-1', userId: 'u1', type: 'EMAIL', subject: 'Test', body: 'Body', status: 'SENT', createdAt: new Date().toISOString() },
        { id: 'n-2', userId: 'u1', type: 'PUSH', subject: 'Alert', body: 'Body', status: 'PENDING', createdAt: new Date().toISOString() },
      ];
      adapterSpy.listByUser.mockReturnValue(of({ items: mockNotifications, total: 2, page: 0, size: 20 }));

      store.loadNotifications('u1');

      expect(store.notifications().length).toBe(2);
      expect(store.total()).toBe(2);
      expect(store.hasNotifications()).toBe(true);
    });

    it('should count unread notifications as those without readAt', () => {
      const mockNotifications = [
        { id: 'n-1', userId: 'u1', type: 'EMAIL', subject: 'T', body: 'B', status: 'SENT', createdAt: new Date().toISOString(), readAt: '2026-01-01T00:00:00Z' },
        { id: 'n-2', userId: 'u1', type: 'PUSH', subject: 'T', body: 'B', status: 'SENT', createdAt: new Date().toISOString(), readAt: null },
        { id: 'n-3', userId: 'u1', type: 'EMAIL', subject: 'T', body: 'B', status: 'SENT', createdAt: new Date().toISOString() },
      ];
      adapterSpy.listByUser.mockReturnValue(of({ items: mockNotifications, total: 3, page: 0, size: 20 }));

      store.loadNotifications('u1');

      expect(store.unreadCount()).toBe(2);
    });

    it('should set error on failure', () => {
      adapterSpy.listByUser.mockReturnValue(throwError(() => ({ detail: 'Forbidden' })));

      store.loadNotifications('u1');

      expect(store.error()).toBe('Forbidden');
      expect(store.loading()).toBe(false);
    });
  });

  describe('markAsRead', () => {
    it('should mark a single notification as read and decrease unreadCount', () => {
      const notification = {
        id: 'n-1', userId: 'u1', type: 'EMAIL', subject: 'T', body: 'B',
        status: 'SENT', createdAt: new Date().toISOString(), readAt: null,
      };
      adapterSpy.listByUser.mockReturnValue(of({ items: [notification], total: 1, page: 0, size: 20 }));
      store.loadNotifications('u1');
      expect(store.unreadCount()).toBe(1);

      const readAtIso = '2026-09-22T10:00:00Z';
      adapterSpy.markRead.mockReturnValue(of({ ...notification, readAt: readAtIso }));

      store.markAsRead('n-1');

      expect(adapterSpy.markRead).toHaveBeenCalledWith('n-1');
      expect(store.notifications()[0].readAt).toBe(readAtIso);
      expect(store.unreadCount()).toBe(0);
    });

    it('should skip already-read notifications without calling the adapter', () => {
      const notification = {
        id: 'n-1', userId: 'u1', type: 'EMAIL', subject: 'T', body: 'B',
        status: 'SENT', createdAt: new Date().toISOString(), readAt: '2026-01-01T00:00:00Z',
      };
      adapterSpy.listByUser.mockReturnValue(of({ items: [notification], total: 1, page: 0, size: 20 }));
      store.loadNotifications('u1');

      store.markAsRead('n-1');

      expect(adapterSpy.markRead).not.toHaveBeenCalled();
      expect(store.unreadCount()).toBe(0);
    });
  });

  describe('markAllAsRead', () => {
    it('should mark all unread notifications as read', () => {
      const mockNotifications = [
        { id: 'n-1', userId: 'u1', type: 'EMAIL', subject: 'T', body: 'B', status: 'SENT', createdAt: new Date().toISOString(), readAt: '2026-01-01T00:00:00Z' },
        { id: 'n-2', userId: 'u1', type: 'PUSH', subject: 'T', body: 'B', status: 'SENT', createdAt: new Date().toISOString(), readAt: null },
        { id: 'n-3', userId: 'u1', type: 'EMAIL', subject: 'T', body: 'B', status: 'SENT', createdAt: new Date().toISOString() },
      ];
      adapterSpy.listByUser.mockReturnValue(of({ items: mockNotifications, total: 3, page: 0, size: 20 }));
      store.loadNotifications('u1');
      expect(store.unreadCount()).toBe(2);

      adapterSpy.markAllRead.mockReturnValue(of({ marked: 2 }));

      store.markAllAsRead('u1');

      expect(adapterSpy.markAllRead).toHaveBeenCalledWith('u1');
      expect(store.unreadCount()).toBe(0);
      expect(store.notifications()[0].readAt).toBe('2026-01-01T00:00:00Z');
      expect(store.notifications()[1].readAt).toBeTruthy();
      expect(store.notifications()[2].readAt).toBeTruthy();
    });

    it('should not call the adapter when there is nothing unread', () => {
      adapterSpy.listByUser.mockReturnValue(of({ items: [], total: 0, page: 0, size: 20 }));
      store.loadNotifications('u1');

      store.markAllAsRead('u1');

      expect(adapterSpy.markAllRead).not.toHaveBeenCalled();
    });
  });

  describe('reset', () => {
    it('should clear all state', () => {
      adapterSpy.listByUser.mockReturnValue(of({ items: [{ id: 'n1' }], total: 1, page: 0, size: 20 }));
      store.loadNotifications('u1');
      expect(store.hasNotifications()).toBe(true);

      store.reset();

      expect(store.notifications()).toEqual([]);
      expect(store.total()).toBe(0);
      expect(store.hasNotifications()).toBe(false);
    });
  });
});
