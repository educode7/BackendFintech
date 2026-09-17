import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MfaLoginComponent } from './mfa-login.component';
import { AuthStore } from '../../../application/stores/auth.store';

describe('MfaLoginComponent', () => {
  let storeSpy: {
    locked: ReturnType<typeof vi.fn>;
    remainingSeconds: ReturnType<typeof vi.fn>;
    attempts: ReturnType<typeof vi.fn>;
    verifying: ReturnType<typeof vi.fn>;
    verifyError: ReturnType<typeof vi.fn>;
    verifyLogin: ReturnType<typeof vi.fn>;
    clearVerifyError: ReturnType<typeof vi.fn>;
    tickLockout: ReturnType<typeof vi.fn>;
    restoreLockout: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    storeSpy = {
      locked: vi.fn().mockReturnValue(false),
      remainingSeconds: vi.fn().mockReturnValue(0),
      attempts: vi.fn().mockReturnValue(0),
      verifying: vi.fn().mockReturnValue(false),
      verifyError: vi.fn().mockReturnValue(null),
      verifyLogin: vi.fn(),
      clearVerifyError: vi.fn(),
      tickLockout: vi.fn(),
      restoreLockout: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [MfaLoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthStore, useValue: storeSpy },
      ],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(MfaLoginComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should call store.verifyLogin on verify with valid code', () => {
    const fixture = TestBed.createComponent(MfaLoginComponent);
    fixture.detectChanges();

    fixture.componentInstance.code = '123456';
    fixture.componentInstance.verify();
    expect(storeSpy.verifyLogin).toHaveBeenCalledWith('123456', { onSuccess: expect.any(Function) });
  });

  it('should not call store.verifyLogin with incomplete code', () => {
    const fixture = TestBed.createComponent(MfaLoginComponent);
    fixture.detectChanges();

    fixture.componentInstance.code = '12345';
    fixture.componentInstance.verify();
    expect(storeSpy.verifyLogin).not.toHaveBeenCalled();
  });

  it('should not call store.verifyLogin when locked', () => {
    storeSpy.locked.mockReturnValue(true);
    const fixture = TestBed.createComponent(MfaLoginComponent);
    fixture.detectChanges();

    fixture.componentInstance.code = '123456';
    fixture.componentInstance.verify();
    expect(storeSpy.verifyLogin).not.toHaveBeenCalled();
  });

  it('should format time correctly', () => {
    const fixture = TestBed.createComponent(MfaLoginComponent);
    expect(fixture.componentInstance.formatTime(0)).toBe('0:00');
    expect(fixture.componentInstance.formatTime(65)).toBe('1:05');
    expect(fixture.componentInstance.formatTime(300)).toBe('5:00');
  });

  it('should render lockout message when locked', () => {
    storeSpy.locked.mockReturnValue(true);
    storeSpy.remainingSeconds.mockReturnValue(300);
    const fixture = TestBed.createComponent(MfaLoginComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.lockout')).toBeTruthy();
    expect(compiled.querySelector('.lockout p')?.textContent).toContain('5:00');
  });

  it('should render verify form when not locked', () => {
    const fixture = TestBed.createComponent(MfaLoginComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.verify-section')).toBeTruthy();
    expect(compiled.querySelector('.btn-primary')?.textContent).toContain('Verify');
  });

  it('should render attempts info', () => {
    storeSpy.attempts.mockReturnValue(2);
    const fixture = TestBed.createComponent(MfaLoginComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.attempts-info')?.textContent).toContain('3');
  });

  it('should render error message', () => {
    storeSpy.verifyError.mockReturnValue('Invalid code');
    const fixture = TestBed.createComponent(MfaLoginComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.field-error')?.textContent).toContain('Invalid code');
  });
});
