import { TestBed } from '@angular/core/testing';
import { NotificationStore } from './notification.store';
import { NotificationAdapter } from '../../infrastructure/notification.adapter';
import { of, throwError } from 'rxjs';

describe('NotificationStore', () => {
  let store: NotificationStore;
  let adapterSpy: { listByUser: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    adapterSpy = { listByUser: vi.fn() };

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
      adapterSpy.listByUser.mockReturnValue(of({ data: mockNotifications, total: 2, page: 0, size: 20 }));

      store.loadNotifications('u1');

      expect(store.notifications().length).toBe(2);
      expect(store.total()).toBe(2);
      expect(store.hasNotifications()).toBe(true);
    });

    it('should count unread notifications', () => {
      const mockNotifications = [
        { id: 'n-1', userId: 'u1', type: 'EMAIL', subject: 'T', body: 'B', status: 'SENT', createdAt: new Date().toISOString() },
        { id: 'n-2', userId: 'u1', type: 'PUSH', subject: 'T', body: 'B', status: 'PENDING', createdAt: new Date().toISOString() },
        { id: 'n-3', userId: 'u1', type: 'EMAIL', subject: 'T', body: 'B', status: 'PENDING', createdAt: new Date().toISOString() },
      ];
      adapterSpy.listByUser.mockReturnValue(of({ data: mockNotifications, total: 3, page: 0, size: 20 }));

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

  describe('reset', () => {
    it('should clear all state', () => {
      adapterSpy.listByUser.mockReturnValue(of({ data: [{ id: 'n1' }], total: 1, page: 0, size: 20 }));
      store.loadNotifications('u1');
      expect(store.hasNotifications()).toBe(true);

      store.reset();

      expect(store.notifications()).toEqual([]);
      expect(store.total()).toBe(0);
      expect(store.hasNotifications()).toBe(false);
    });
  });
});
