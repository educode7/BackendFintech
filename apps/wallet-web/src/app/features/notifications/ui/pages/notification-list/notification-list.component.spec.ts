import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { NotificationListComponent } from './notification-list.component';
import { NotificationStore } from '../../../application/stores/notification.store';
import { AuthService } from '@core/infrastructure/auth.service';

describe('NotificationListComponent', () => {
  let authServiceMock: Partial<AuthService>;
  let unreadCountValue: number;
  let storeMock: {
    loading: () => boolean;
    error: () => string | null;
    notifications: () => unknown[];
    unreadCount: () => number;
    loadNotifications: ReturnType<typeof vi.fn>;
    markAsRead: ReturnType<typeof vi.fn>;
    markAllAsRead: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authServiceMock = {
      getUserInfo: vi.fn().mockReturnValue({ sub: 'test-user-id', email: 'test@test.com', roles: [], expiresAt: 0 }),
      getCachedUserInfo: vi.fn().mockReturnValue({ sub: 'test-user-id', email: 'test@test.com', roles: [], expiresAt: 0 }),
      isAuthenticated: vi.fn().mockReturnValue(true),
    };
    unreadCountValue = 0;
    storeMock = {
      loading: () => false,
      error: () => null,
      notifications: () => [],
      unreadCount: () => unreadCountValue,
      loadNotifications: vi.fn(),
      markAsRead: vi.fn(),
      markAllAsRead: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [NotificationListComponent],
      providers: [
        provideRouter([]),
        { provide: NotificationStore, useValue: storeMock },
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(NotificationListComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render page header', () => {
    const fixture = TestBed.createComponent(NotificationListComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-page-header')).toBeTruthy();
  });

  it('should render notification list container', () => {
    const fixture = TestBed.createComponent(NotificationListComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.notification-list')).toBeTruthy();
  });

  it('should NOT show mark-all button when there are no unread notifications', () => {
    const fixture = TestBed.createComponent(NotificationListComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.mark-all-btn')).toBeNull();
  });

  it('should show mark-all button when unreadCount > 0 and wire it to the store', () => {
    unreadCountValue = 2;
    const fixture = TestBed.createComponent(NotificationListComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const button = compiled.querySelector('.mark-all-btn') as HTMLButtonElement;

    expect(button).toBeTruthy();
    expect(button.textContent).toContain('Mark all as read');

    button.click();
    expect(storeMock.markAllAsRead).toHaveBeenCalledWith('test-user-id');
  });

  it('should mark a notification read on card click', () => {
    storeMock.notifications = () => [
      { id: 'n-1', subject: 'Hello', body: 'World', type: 'EMAIL', status: 'SENT', createdAt: new Date().toISOString(), readAt: null },
    ];
    const fixture = TestBed.createComponent(NotificationListComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const card = compiled.querySelector('.notification-card') as HTMLElement;

    expect(card).toBeTruthy();
    card.click();
    expect(storeMock.markAsRead).toHaveBeenCalledWith('n-1');
  });
});
