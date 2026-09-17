package com.wallet.auth.application.service;

import com.wallet.auth.application.port.out.KeycloakMfaPort;
import com.wallet.auth.domain.MfaSetup;
import com.wallet.auth.domain.MfaVerification;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class MfaSetupServiceTest {

    @InjectMock
    KeycloakMfaPort keycloakMfaPort;

    @Inject
    MfaSetupService mfaSetupService;

    @Inject
    MfaVerificationService mfaVerificationService;

    @Inject
    MfaDisableService mfaDisableService;

    @BeforeEach
    void setUp() {
        reset(keycloakMfaPort);
    }

    // --- Setup tests ---

    @Test
    void shouldReturnSetupDataOnSuccess() {
        var setup = new MfaSetup("data:image/png;base64,abc", "SECRET123",
                List.of("code1", "code2"), "WalletApp", "user@test.com");
        when(keycloakMfaPort.initiateSetup("user-1"))
                .thenReturn(Uni.createFrom().item(setup));

        var result = mfaSetupService.setup("user-1").await().indefinitely();

        assertNotNull(result);
        assertEquals("SECRET123", result.secret());
        assertEquals(2, result.recoveryCodes().size());
        assertEquals("WalletApp", result.issuer());
    }

    @Test
    void shouldFailOnNullUserId() {
        try {
            mfaSetupService.setup(null).await().indefinitely();
            fail("Expected IllegalArgumentException");
        } catch (Exception e) {
            assertInstanceOf(IllegalArgumentException.class, e);
        }
    }

    @Test
    void shouldFailOnBlankUserId() {
        try {
            mfaSetupService.setup("  ").await().indefinitely();
            fail("Expected exception");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    // --- Verification tests ---

    @Test
    void shouldVerifyValidCode() {
        var verification = new MfaVerification(true, 10);
        when(keycloakMfaPort.verifyCode("user-1", "123456"))
                .thenReturn(Uni.createFrom().item(verification));

        var result = mfaVerificationService.verify("user-1", "123456").await().indefinitely();

        assertTrue(result.verified());
        assertEquals(10, result.backupCodesRemaining());
    }

    @Test
    void shouldReturnUnverifiedForInvalidCode() {
        var verification = new MfaVerification(false, 10);
        when(keycloakMfaPort.verifyCode("user-1", "000000"))
                .thenReturn(Uni.createFrom().item(verification));

        var result = mfaVerificationService.verify("user-1", "000000").await().indefinitely();

        assertFalse(result.verified());
    }

    @Test
    void shouldFailVerificationOnNullCode() {
        try {
            mfaVerificationService.verify("user-1", null).await().indefinitely();
            fail("Expected IllegalArgumentException");
        } catch (Exception e) {
            assertInstanceOf(IllegalArgumentException.class, e);
        }
    }

    // --- Disable tests ---

    @Test
    void shouldDisableMfaSuccessfully() {
        when(keycloakMfaPort.disableMfa("user-1", "123456"))
                .thenReturn(Uni.createFrom().voidItem());

        assertDoesNotThrow(() ->
                mfaDisableService.disable("user-1", "123456").await().indefinitely());
    }

    @Test
    void shouldFailDisableOnInvalidCode() {
        when(keycloakMfaPort.disableMfa("user-1", "bad"))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Invalid code")));

        try {
            mfaDisableService.disable("user-1", "bad").await().indefinitely();
            fail("Expected exception");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    void shouldFailDisableOnNullCode() {
        try {
            mfaDisableService.disable("user-1", null).await().indefinitely();
            fail("Expected IllegalArgumentException");
        } catch (Exception e) {
            assertInstanceOf(IllegalArgumentException.class, e);
        }
    }
}
