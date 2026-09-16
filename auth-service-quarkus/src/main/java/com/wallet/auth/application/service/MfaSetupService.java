package com.wallet.auth.application.service;

import com.wallet.auth.application.port.out.KeycloakMfaPort;
import com.wallet.auth.domain.MfaSetup;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use case: initiate MFA setup — generates QR code, secret, and recovery codes.
 */
@ApplicationScoped
public class MfaSetupService {

    private static final Logger log = LoggerFactory.getLogger(MfaSetupService.class);

    @Inject
    KeycloakMfaPort keycloakMfaPort;

    public Uni<MfaSetup> setup(String userId) {
        if (userId == null || userId.isBlank()) {
            return Uni.createFrom().failure(
                    new IllegalArgumentException("User ID is required"));
        }

        log.debug("Initiating MFA setup for user");
        return keycloakMfaPort.initiateSetup(userId)
                .onFailure().transform(f -> {
                    log.error("MFA setup failed: {}", f.getMessage());
                    return f;
                });
    }
}
