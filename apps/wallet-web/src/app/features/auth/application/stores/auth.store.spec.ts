import { AuthStore } from './auth.store';
import { of, throwError } from 'rxjs';

describe('AuthStore', () => {
  let store: AuthStore;
  let adapterSpy: {
    setup: ReturnType<typeof vi.fn>;
    verify: ReturnType<typeof vi.fn>;
    disable: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    adapterSpy = {
      setup: vi.fn(),
      verify: vi.fn(),
      disable: vi.fn(),
    };
    store = new AuthStore(adapterSpy as never);
  });

  it('should be created', () => {
    expect(store).toBeTruthy();
  });

  it('should start with empty state', () => {
    expect(store.setupData()).toBeNull();
    expect(store.setupLoading()).toBe(false);
    expect(store.setupError()).toBeNull();
    expect(store.setupComplete()).toBe(false);
    expect(store.verifying()).toBe(false);
    expect(store.verifyError()).toBeNull();
    expect(store.disableLoading()).toBe(false);
    expect(store.disableError()).toBeNull();
    expect(store.disableSuccess()).toBe(false);
    expect(store.locked()).toBe(false);
    expect(store.remainingSeconds()).toBe(0);
    expect(store.attempts()).toBe(0);
  });

  describe('initSetup', () => {
    it('should load setup data', () => {
      const mockData = {
        qr_code: 'data:image/png;base64,abc',
        secret: 'SECRET',
        recovery_codes: ['c1', 'c2'],
        issuer: 'WalletApp',
        account_name: 'user@test.com',
      };
      adapterSpy.setup.mockReturnValue(of(mockData));

      store.initSetup();

      expect(store.setupData()?.secret).toBe('SECRET');
      expect(store.setupLoading()).toBe(false);
      expect(store.setupError()).toBeNull();
    });

    it('should set error on failure', () => {
      adapterSpy.setup.mockReturnValue(throwError(() => ({ detail: 'Setup failed' })));

      store.initSetup();

      expect(store.setupError()).toBe('Setup failed');
      expect(store.setupLoading()).toBe(false);
    });
  });

  describe('verifySetup', () => {
    it('should set setupComplete on successful verification', () => {
      adapterSpy.verify.mockReturnValue(of({ verified: true, backup_codes_remaining: 10 }));

      store.verifySetup('123456');

      expect(store.setupComplete()).toBe(true);
      expect(store.verifying()).toBe(false);
    });

    it('should set error on invalid code', () => {
      adapterSpy.verify.mockReturnValue(of({ verified: false, backup_codes_remaining: 10 }));

      store.verifySetup('000000');

      expect(store.verifyError()).toBe('Invalid code. Please try again.');
      expect(store.setupComplete()).toBe(false);
    });

    it('should set error on HTTP failure', () => {
      adapterSpy.verify.mockReturnValue(throwError(() => ({ detail: 'Server error' })));

      store.verifySetup('123456');

      expect(store.verifyError()).toBe('Server error');
      expect(store.verifying()).toBe(false);
    });
  });

  describe('verifyLogin', () => {
    it('should call onSuccess on verified', () => {
      const onSuccess = vi.fn();
      adapterSpy.verify.mockReturnValue(of({ verified: true, backup_codes_remaining: 10 }));

      store.verifyLogin('123456', { onSuccess });

      expect(onSuccess).toHaveBeenCalledOnce();
      expect(store.verifying()).toBe(false);
    });

    it('should handle failure and increment attempts', () => {
      adapterSpy.verify.mockReturnValue(of({ verified: false, backup_codes_remaining: 10 }));

      store.verifyLogin('000000');

      expect(store.attempts()).toBe(1);
      expect(store.verifyError()).toBe('Invalid code. Please try again.');
    });

    it('should lock after max attempts', () => {
      adapterSpy.verify.mockReturnValue(of({ verified: false, backup_codes_remaining: 10 }));

      for (let i = 0; i < 5; i++) {
        store.verifyLogin('000000');
      }

      expect(store.locked()).toBe(true);
      expect(store.remainingSeconds()).toBe(300);
    });

    it('should handle HTTP error', () => {
      adapterSpy.verify.mockReturnValue(throwError(() => ({ detail: 'Server error' })));

      store.verifyLogin('123456');

      expect(store.verifyError()).toBe('Server error');
      expect(store.verifying()).toBe(false);
    });
  });

  describe('disableMfa', () => {
    it('should set disableSuccess on success', () => {
      adapterSpy.disable.mockReturnValue(of(undefined));

      store.disableMfa('123456');

      expect(store.disableSuccess()).toBe(true);
      expect(store.disableLoading()).toBe(false);
    });

    it('should set error on failure', () => {
      adapterSpy.disable.mockReturnValue(throwError(() => ({ detail: 'Invalid code' })));

      store.disableMfa('000000');

      expect(store.disableError()).toBe('Invalid code');
      expect(store.disableLoading()).toBe(false);
    });
  });

  describe('handleFailure', () => {
    it('should increment attempts and set error', () => {
      store.handleFailure('Invalid code');

      expect(store.attempts()).toBe(1);
      expect(store.verifyError()).toBe('Invalid code');
    });

    it('should lock after 5 attempts', () => {
      for (let i = 0; i < 5; i++) {
        store.handleFailure('Invalid code');
      }

      expect(store.locked()).toBe(true);
      expect(store.remainingSeconds()).toBe(300);
    });
  });

  describe('tickLockout', () => {
    it('should decrement remaining seconds', () => {
      store.handleFailure('fail');
      store.handleFailure('fail');
      store.handleFailure('fail');
      store.handleFailure('fail');
      store.handleFailure('fail');

      expect(store.locked()).toBe(true);
      store.tickLockout();
      expect(store.remainingSeconds()).toBe(299);
    });

    it('should unlock when remaining reaches 0', () => {
      store.handleFailure('fail');
      store.handleFailure('fail');
      store.handleFailure('fail');
      store.handleFailure('fail');
      store.handleFailure('fail');

      // Simulate fast-forward
      for (let i = 0; i < 300; i++) {
        store.tickLockout();
      }

      expect(store.locked()).toBe(false);
      expect(store.attempts()).toBe(0);
      expect(store.remainingSeconds()).toBe(0);
    });
  });

  describe('clear methods', () => {
    it('should clear setup error', () => {
      adapterSpy.setup.mockReturnValue(throwError(() => ({ detail: 'error' })));
      store.initSetup();
      expect(store.setupError()).toBeTruthy();

      store.clearSetupError();
      expect(store.setupError()).toBeNull();
    });

    it('should clear verify error', () => {
      adapterSpy.verify.mockReturnValue(of({ verified: false, backup_codes_remaining: 10 }));
      store.verifySetup('000000');
      expect(store.verifyError()).toBeTruthy();

      store.clearVerifyError();
      expect(store.verifyError()).toBeNull();
    });

    it('should clear disable error', () => {
      adapterSpy.disable.mockReturnValue(throwError(() => ({ detail: 'error' })));
      store.disableMfa('000000');
      expect(store.disableError()).toBeTruthy();

      store.clearDisableError();
      expect(store.disableError()).toBeNull();
    });
  });

  describe('reset', () => {
    it('should clear all state', () => {
      adapterSpy.setup.mockReturnValue(of({
        qr_code: 'abc', secret: 'SECRET', recovery_codes: ['c1'],
        issuer: 'WalletApp', account_name: 'user@test.com',
      }));
      store.initSetup();
      expect(store.setupData()).toBeTruthy();

      store.reset();

      expect(store.setupData()).toBeNull();
      expect(store.setupLoading()).toBe(false);
      expect(store.setupError()).toBeNull();
      expect(store.setupComplete()).toBe(false);
      expect(store.verifying()).toBe(false);
      expect(store.verifyError()).toBeNull();
      expect(store.disableLoading()).toBe(false);
      expect(store.disableError()).toBeNull();
      expect(store.disableSuccess()).toBe(false);
      expect(store.locked()).toBe(false);
      expect(store.remainingSeconds()).toBe(0);
      expect(store.attempts()).toBe(0);
    });
  });

  describe('restoreLockout', () => {
    it('should restore lockout from future timestamp', () => {
      const futureTimestamp = Date.now() + 300_000; // 5 minutes from now

      store.restoreLockout(futureTimestamp);

      expect(store.locked()).toBe(true);
      expect(store.remainingSeconds()).toBeGreaterThan(0);
      expect(store.attempts()).toBe(5);
    });

    it('should not lock if timestamp is in the past', () => {
      const pastTimestamp = Date.now() - 1000;

      store.restoreLockout(pastTimestamp);

      expect(store.locked()).toBe(false);
    });
  });
});
