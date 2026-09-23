import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NotificationAdapter } from './notification.adapter';
import { environment } from '@env/environment';

describe('NotificationAdapter', () => {
  let adapter: NotificationAdapter;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        NotificationAdapter,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    adapter = TestBed.inject(NotificationAdapter);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(adapter).toBeTruthy();
  });

  describe('listByUser', () => {
    it('should GET /api/v1/notifications/:userId with pagination', () => {
      const mockResponse = {
        items: [
          {
            id: 'n-1',
            userId: 'user-1',
            type: 'EMAIL',
            subject: 'Payment completed',
            body: 'Your payment was processed',
            status: 'SENT',
            createdAt: new Date().toISOString(),
          },
        ],
        total: 1,
        page: 0,
        size: 20,
      };

      adapter.listByUser('user-1', 0, 20).subscribe((response) => {
        expect(response.items.length).toBe(1);
        expect(response.total).toBe(1);
        expect(response.items[0].type).toBe('EMAIL');
      });

      const req = httpMock.expectOne(
        (r) => r.url === `${environment.apiGateway}/api/v1/notifications/user-1`
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('20');
      req.flush(mockResponse);
    });
  });

  describe('markRead', () => {
    it('should PATCH /api/v1/notifications/:id/read', () => {
      const mockResponse = {
        id: 'n-1',
        userId: 'user-1',
        type: 'EMAIL',
        subject: 'Payment completed',
        body: 'Your payment was processed',
        status: 'SENT',
        createdAt: new Date().toISOString(),
        sentAt: new Date().toISOString(),
        readAt: new Date().toISOString(),
      };

      adapter.markRead('n-1').subscribe((response) => {
        expect(response.id).toBe('n-1');
        expect(response.readAt).toBeTruthy();
      });

      const req = httpMock.expectOne(
        (r) => r.url === `${environment.apiGateway}/api/v1/notifications/n-1/read`
      );
      expect(req.request.method).toBe('PATCH');
      req.flush(mockResponse);
    });
  });

  describe('markAllRead', () => {
    it('should PATCH /api/v1/notifications/:userId/read-all returning marked count', () => {
      adapter.markAllRead('user-1').subscribe((response) => {
        expect(response.marked).toBe(2);
      });

      const req = httpMock.expectOne(
        (r) => r.url === `${environment.apiGateway}/api/v1/notifications/user-1/read-all`
      );
      expect(req.request.method).toBe('PATCH');
      req.flush({ marked: 2 });
    });
  });
});
