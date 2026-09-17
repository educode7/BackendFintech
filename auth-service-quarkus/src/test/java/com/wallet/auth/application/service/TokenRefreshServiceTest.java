package com.wallet.auth.application.service;

import com.wallet.auth.application.port.out.KeycloakTokenPort;
import com.wallet.auth.domain.TokenPair;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class TokenRefreshServiceTest {

    @InjectMock
    KeycloakTokenPort keycloakTokenPort;

    @Inject
    TokenRefreshService tokenRefreshService;

    @BeforeEach
    void setUp() {
        reset(keycloakTokenPort);
    }

    @Test
    void shouldReturnTokensOnValidRefresh() {
        var pair = TokenPair.of("new-access", "new-refresh", 300);
        when(keycloakTokenPort.exchangeRefreshToken("valid-token"))
                .thenReturn(Uni.createFrom().item(pair));

        var result = tokenRefreshService.refresh("valid-token").await().indefinitely();

        assertNotNull(result);
        assertEquals("new-access", result.accessToken());
        assertEquals("new-refresh", result.refreshToken());
        assertEquals(300, result.expiresIn());
        verify(keycloakTokenPort).exchangeRefreshToken("valid-token");
    }

    @Test
    void shouldFailOnNullRefreshToken() {
        try {
            tokenRefreshService.refresh(null).await().indefinitely();
            fail("Expected IllegalArgumentException");
        } catch (Exception e) {
            assertInstanceOf(IllegalArgumentException.class, e);
        }
        verify(keycloakTokenPort, never()).exchangeRefreshToken(any());
    }

    @Test
    void shouldFailOnBlankRefreshToken() {
        try {
            tokenRefreshService.refresh("  ").await().indefinitely();
            fail("Expected IllegalArgumentException");
        } catch (Exception e) {
            assertInstanceOf(IllegalArgumentException.class, e);
        }
    }

    @Test
    void shouldPropagateKeycloakFailure() {
        when(keycloakTokenPort.exchangeRefreshToken("expired-token"))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Token expired")));

        try {
            tokenRefreshService.refresh("expired-token").await().indefinitely();
            fail("Expected RuntimeException");
        } catch (Exception e) {
            assertEquals("Token expired", e.getMessage());
        }
    }
}
