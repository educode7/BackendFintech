import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MfaSetupComponent } from './mfa-setup.component';
import { AuthStore } from '../../../application/stores/auth.store';

describe('MfaSetupComponent', () => {
  let storeSpy: {
    setupLoading: ReturnType<typeof vi.fn>;
    setupError: ReturnType<typeof vi.fn>;
    setupData: ReturnType<typeof vi.fn>;
    setupComplete: ReturnType<typeof vi.fn>;
    verifying: ReturnType<typeof vi.fn>;
    verifyError: ReturnType<typeof vi.fn>;
    initSetup: ReturnType<typeof vi.fn>;
    verifySetup: ReturnType<typeof vi.fn>;
    clearVerifyError: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    storeSpy = {
      setupLoading: vi.fn().mockReturnValue(false),
      setupError: vi.fn().mockReturnValue(null),
      setupData: vi.fn().mockReturnValue(null),
      setupComplete: vi.fn().mockReturnValue(false),
      verifying: vi.fn().mockReturnValue(false),
      verifyError: vi.fn().mockReturnValue(null),
      initSetup: vi.fn(),
      verifySetup: vi.fn(),
      clearVerifyError: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [MfaSetupComponent],
      providers: [
        provideRouter([]),
        { provide: AuthStore, useValue: storeSpy },
      ],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(MfaSetupComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should call store.initSetup on init', () => {
    const fixture = TestBed.createComponent(MfaSetupComponent);
    fixture.detectChanges();
    expect(storeSpy.initSetup).toHaveBeenCalledOnce();
  });

  it('should call store.initSetup on retry', () => {
    storeSpy.setupError.mockReturnValue('Some error');
    const fixture = TestBed.createComponent(MfaSetupComponent);
    fixture.detectChanges();

    fixture.componentInstance.initSetup();
    expect(storeSpy.initSetup).toHaveBeenCalledTimes(2);
  });

  it('should call store.verifySetup with code', () => {
    const fixture = TestBed.createComponent(MfaSetupComponent);
    fixture.detectChanges();

    fixture.componentInstance.verifyCode = '123456';
    fixture.componentInstance.verifySetup();
    expect(storeSpy.verifySetup).toHaveBeenCalledWith('123456');
  });

  it('should not call store.verifySetup with incomplete code', () => {
    const fixture = TestBed.createComponent(MfaSetupComponent);
    fixture.detectChanges();

    fixture.componentInstance.verifyCode = '12345';
    fixture.componentInstance.verifySetup();
    expect(storeSpy.verifySetup).not.toHaveBeenCalled();
  });

  it('should render loading state', () => {
    storeSpy.setupLoading.mockReturnValue(true);
    const fixture = TestBed.createComponent(MfaSetupComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.loading')?.textContent).toContain('Setting up MFA');
  });

  it('should render error state with retry button', () => {
    storeSpy.setupError.mockReturnValue('Setup failed');
    const fixture = TestBed.createComponent(MfaSetupComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.error')?.textContent).toContain('Setup failed');
    expect(compiled.querySelector('button')?.textContent).toContain('Retry');
  });

  it('should render QR code when setupData is present', () => {
    storeSpy.setupData.mockReturnValue({
      qr_code: 'data:image/png;base64,abc',
      secret: 'SECRET',
      recovery_codes: ['c1', 'c2'],
      issuer: 'WalletApp',
      account_name: 'user@test.com',
    });
    const fixture = TestBed.createComponent(MfaSetupComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.qr-image')).toBeTruthy();
    expect(compiled.querySelector('.secret-box code')?.textContent).toContain('SECRET');
  });
});
