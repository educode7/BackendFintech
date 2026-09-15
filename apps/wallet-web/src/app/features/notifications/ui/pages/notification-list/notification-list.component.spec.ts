import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NotificationListComponent } from './notification-list.component';
import { NotificationStore } from '../../../application/stores/notification.store';

describe('NotificationListComponent', () => {
  beforeEach(async () => {
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
