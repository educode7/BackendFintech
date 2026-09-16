package com.wallet.auth.application.port.out;

import com.wallet.auth.domain.MfaSetup;
import com.wallet.auth.domain.MfaVerification;
import io.smallrye.mutiny.Uni;

/**
 * Outbound port for Keycloak MFA operations — setup, verification, and disable.
 */
public interface KeycloakMfaPort {

    Uni<MfaSetup> initiateSetup(String userId);

    Uni<MfaVerification> verifyCode(String userId, String code);

    Uni<Void> disableMfa(String userId, String code);
}
