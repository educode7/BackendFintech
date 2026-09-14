import { TestBed } from '@angular/core/testing';
import { ErrorDisplayComponent } from './error-display.component';

describe('ErrorDisplayComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ErrorDisplayComponent],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(ErrorDisplayComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render default title', () => {
    const fixture = TestBed.createComponent(ErrorDisplayComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h3')?.textContent).toContain('Error');
  });

  it('should render custom detail', () => {
    const fixture = TestBed.createComponent(ErrorDisplayComponent);
    fixture.componentRef.setInput('detail', 'Something went wrong');
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.error-detail')?.textContent).toContain('Something went wrong');
  });

  it('should render correlation ID when provided', () => {
    const fixture = TestBed.createComponent(ErrorDisplayComponent);
    fixture.componentRef.setInput('correlationId', 'corr-123');
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.error-meta')?.textContent).toContain('corr-123');
  });
});
