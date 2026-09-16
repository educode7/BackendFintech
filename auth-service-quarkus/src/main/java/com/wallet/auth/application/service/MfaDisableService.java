package com.wallet.auth.application.service;

import com.wallet.auth.application.port.out.KeycloakMfaPort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use case: disable MFA — requires valid TOTP code for confirmation.
 */
@ApplicationScoped
public class MfaDisableService {

    private static final Logger log = LoggerFactory.getLogger(MfaDisableService.class);

    @Inject
    KeycloakMfaPort keycloakMfaPort;

    public Uni<Void> disable(String userId, String code) {
        if (userId == null || userId.isBlank()) {
            return Uni.createFrom().failure(
                    new IllegalArgumentException("User ID is required"));
        }
        if (code == null || code.isBlank()) {
            return Uni.createFrom().failure(
                    new IllegalArgumentException("MFA code is required"));
        }

        log.debug("Disabling MFA for user");
        return keycloakMfaPort.disableMfa(userId, code)
                .onItem().invoke(() -> log.debug("MFA disabled successfully"))
                .onFailure().transform(f -> {
                    log.error("MFA disable failed: {}", f.getMessage());
                    return f;
                });
    }
}
