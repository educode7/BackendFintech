package com.wallet.auth.application.service;

import com.wallet.auth.application.port.out.KeycloakTokenPort;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class TokenRevocationServiceTest {

    @InjectMock
    KeycloakTokenPort keycloakTokenPort;

    @Inject
    TokenRevocationService tokenRevocationService;

    @BeforeEach
    void setUp() {
        reset(keycloakTokenPort);
    }

    @Test
    void shouldRevokeTokenSuccessfully() {
        when(keycloakTokenPort.revokeRefreshToken("valid-token"))
                .thenReturn(Uni.createFrom().voidItem());

        assertDoesNotThrow(() ->
                tokenRevocationService.revoke("valid-token").await().indefinitely());
        verify(keycloakTokenPort).revokeRefreshToken("valid-token");
    }

    @Test
    void shouldFailOnNullRefreshToken() {
        try {
            tokenRevocationService.revoke(null).await().indefinitely();
            fail("Expected IllegalArgumentException");
        } catch (Exception e) {
            assertInstanceOf(IllegalArgumentException.class, e);
        }
        verify(keycloakTokenPort, never()).revokeRefreshToken(any());
    }

    @Test
    void shouldPropagateRevocationFailure() {
        when(keycloakTokenPort.revokeRefreshToken("bad-token"))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Revocation failed")));

        try {
            tokenRevocationService.revoke("bad-token").await().indefinitely();
            fail("Expected RuntimeException");
        } catch (Exception e) {
            assertEquals("Revocation failed", e.getMessage());
        }
    }
}
