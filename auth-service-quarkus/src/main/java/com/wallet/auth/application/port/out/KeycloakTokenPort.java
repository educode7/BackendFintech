package com.wallet.auth.application.port.out;

import com.wallet.auth.domain.TokenPair;
import io.smallrye.mutiny.Uni;

/**
 * Outbound port for Keycloak token operations — exchange and revocation.
 */
public interface KeycloakTokenPort {

    Uni<TokenPair> exchangeRefreshToken(String refreshToken);

    Uni<Void> revokeRefreshToken(String refreshToken);
}
