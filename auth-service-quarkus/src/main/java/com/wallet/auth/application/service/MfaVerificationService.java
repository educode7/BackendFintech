package com.wallet.auth.application.service;

import com.wallet.auth.application.port.out.KeycloakMfaPort;
import com.wallet.auth.domain.MfaVerification;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use case: verify a TOTP code during MFA setup or login challenge.
 */
@ApplicationScoped
public class MfaVerificationService {

    private static final Logger log = LoggerFactory.getLogger(MfaVerificationService.class);

    @Inject
    KeycloakMfaPort keycloakMfaPort;

    public Uni<MfaVerification> verify(String userId, String code) {
        if (userId == null || userId.isBlank()) {
            return Uni.createFrom().failure(
                    new IllegalArgumentException("User ID is required"));
        }
        if (code == null || code.isBlank()) {
            return Uni.createFrom().failure(
                    new IllegalArgumentException("MFA code is required"));
        }

        log.debug("Verifying MFA code");
        return keycloakMfaPort.verifyCode(userId, code)
                .onFailure().transform(f -> {
                    log.error("MFA verification failed: {}", f.getMessage());
                    return f;
                });
    }
}
