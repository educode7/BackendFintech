import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NotificationService } from './notification.service';
import { environment } from '@env/environment';

describe('NotificationService', () => {
  let service: NotificationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        NotificationService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(NotificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('listByUser', () => {
    it('should GET /api/v1/notifications/:userId with pagination', () => {
      const mockResponse = {
        data: [
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

      service.listByUser('user-1', 0, 20).subscribe((response) => {
        expect(response.data.length).toBe(1);
        expect(response.total).toBe(1);
        expect(response.data[0].type).toBe('EMAIL');
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
});
