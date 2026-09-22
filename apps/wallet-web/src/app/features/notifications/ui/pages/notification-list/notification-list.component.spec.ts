import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { NotificationListComponent } from './notification-list.component';
import { NotificationStore } from '../../../application/stores/notification.store';
import { AuthService } from '@core/infrastructure/auth.service';

describe('NotificationListComponent', () => {
  let authServiceMock: Partial<AuthService>;

  beforeEach(async () => {
    authServiceMock = {
      getUserInfo: vi.fn().mockReturnValue({ sub: 'test-user-id', email: 'test@test.com', roles: [], expiresAt: 0 }),
      getCachedUserInfo: vi.fn().mockReturnValue({ sub: 'test-user-id', email: 'test@test.com', roles: [], expiresAt: 0 }),
      isAuthenticated: vi.fn().mockReturnValue(true),
    };

    await TestBed.configureTestingModule({
      imports: [NotificationListComponent],
      providers: [
        provideRouter([]),
        {
          provide: NotificationStore,
          useValue: {
            loading: () => false,
            error: () => null,
            notifications: () => [],
            loadNotifications: () => {},
          },
        },
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
});
