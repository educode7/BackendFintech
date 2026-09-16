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
        var failure = tokenRefreshService.refresh(null).await().failure();
        assertTrue(failure.isPresent());
        assertInstanceOf(IllegalArgumentException.class, failure.get());
        verify(keycloakTokenPort, never()).exchangeRefreshToken(any());
    }

    @Test
    void shouldFailOnBlankRefreshToken() {
        var failure = tokenRefreshService.refresh("  ").await().failure();
        assertTrue(failure.isPresent());
        assertInstanceOf(IllegalArgumentException.class, failure.get());
    }

    @Test
    void shouldPropagateKeycloakFailure() {
        when(keycloakTokenPort.exchangeRefreshToken("expired-token"))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Token expired")));

        var failure = tokenRefreshService.refresh("expired-token").await().failure();
        assertTrue(failure.isPresent());
        assertEquals("Token expired", failure.get().getMessage());
    }
}
