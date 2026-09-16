package com.wallet.auth.application.service;

import com.wallet.auth.application.port.out.KeycloakTokenPort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use case: revoke a refresh token, invalidating it in Keycloak.
 */
@ApplicationScoped
public class TokenRevocationService {

    private static final Logger log = LoggerFactory.getLogger(TokenRevocationService.class);

    @Inject
    KeycloakTokenPort keycloakTokenPort;

    public Uni<Void> revoke(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Uni.createFrom().failure(
                    new IllegalArgumentException("Refresh token is required"));
        }

        log.debug("Attempting token revocation");
        return keycloakTokenPort.revokeRefreshToken(refreshToken)
                .onItem().invoke(() -> log.debug("Token revoked successfully"))
                .onFailure().transform(f -> {
                    log.error("Token revocation failed: {}", f.getMessage());
                    return f;
                });
    }
}
