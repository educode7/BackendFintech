package com.wallet.auth.application.service;

import com.wallet.auth.application.port.out.KeycloakTokenPort;
import com.wallet.auth.domain.TokenPair;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use case: exchange a valid refresh token for a new token pair.
 * Stateless — delegates token lifecycle entirely to Keycloak.
 */
@ApplicationScoped
public class TokenRefreshService {

    private static final Logger log = LoggerFactory.getLogger(TokenRefreshService.class);

    @Inject
    KeycloakTokenPort keycloakTokenPort;

    public Uni<TokenPair> refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Uni.createFrom().failure(
                    new IllegalArgumentException("Refresh token is required"));
        }

        log.debug("Attempting token refresh");
        return keycloakTokenPort.exchangeRefreshToken(refreshToken)
                .onFailure().transform(f -> {
                    log.error("Token refresh failed: {}", f.getMessage());
                    return f;
                });
    }
}
