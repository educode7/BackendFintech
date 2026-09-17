import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MfaDisableComponent } from './mfa-disable.component';
import { AuthStore } from '../../../application/stores/auth.store';

describe('MfaDisableComponent', () => {
  let storeSpy: {
    disableLoading: ReturnType<typeof vi.fn>;
    disableError: ReturnType<typeof vi.fn>;
    disableSuccess: ReturnType<typeof vi.fn>;
    disableMfa: ReturnType<typeof vi.fn>;
    clearDisableError: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    storeSpy = {
      disableLoading: vi.fn().mockReturnValue(false),
      disableError: vi.fn().mockReturnValue(null),
      disableSuccess: vi.fn().mockReturnValue(false),
      disableMfa: vi.fn(),
      clearDisableError: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [MfaDisableComponent],
      providers: [
        provideRouter([]),
        { provide: AuthStore, useValue: storeSpy },
      ],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(MfaDisableComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should call store.disableMfa with code', () => {
    const fixture = TestBed.createComponent(MfaDisableComponent);
    fixture.detectChanges();

    fixture.componentInstance.code = '123456';
    fixture.componentInstance.disableMfa();
    expect(storeSpy.disableMfa).toHaveBeenCalledWith('123456');
  });

  it('should not call store.disableMfa with incomplete code', () => {
    const fixture = TestBed.createComponent(MfaDisableComponent);
    fixture.detectChanges();

    fixture.componentInstance.code = '12345';
    fixture.componentInstance.disableMfa();
    expect(storeSpy.disableMfa).not.toHaveBeenCalled();
  });

  it('should render success message when disableSuccess is true', () => {
    storeSpy.disableSuccess.mockReturnValue(true);
    const fixture = TestBed.createComponent(MfaDisableComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.success-message')).toBeTruthy();
    expect(compiled.querySelector('.success-message p')?.textContent).toContain('MFA has been disabled');
  });

  it('should render warning and form when not success', () => {
    const fixture = TestBed.createComponent(MfaDisableComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.warning-box')).toBeTruthy();
    expect(compiled.querySelector('.verify-section')).toBeTruthy();
    expect(compiled.querySelector('.btn-danger')?.textContent).toContain('Disable MFA');
  });

  it('should render error message', () => {
    storeSpy.disableError.mockReturnValue('Invalid code');
    const fixture = TestBed.createComponent(MfaDisableComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.field-error')?.textContent).toContain('Invalid code');
  });

  it('should disable button while loading', () => {
    storeSpy.disableLoading.mockReturnValue(true);
    const fixture = TestBed.createComponent(MfaDisableComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const btn = compiled.querySelector('.btn-danger') as HTMLButtonElement;
    expect(btn.disabled).toBe(true);
    expect(btn.textContent).toContain('Disabling...');
  });
});
