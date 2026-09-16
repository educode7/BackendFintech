package com.wallet.auth.infrastructure.keycloak;

import com.wallet.auth.application.port.out.KeycloakTokenPort;
import com.wallet.auth.domain.TokenPair;
import com.wallet.auth.domain.exception.InvalidRefreshTokenException;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Keycloak adapter for token exchange and revocation via OpenID Connect endpoints.
 */
@ApplicationScoped
public class KeycloakTokenAdapter implements KeycloakTokenPort {

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String authServerUrl;

    @ConfigProperty(name = "quarkus.oidc.client-id")
    String clientId;

    @ConfigProperty(name = "quarkus.oidc.credentials.secret", defaultValue = "")
    String clientSecret;

    private final WebClient webClient;

    @Inject
    public KeycloakTokenAdapter(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Uni<TokenPair> exchangeRefreshToken(String refreshToken) {
        Log.debugf("Exchanging refresh token (hint: %s)", hint(refreshToken));

        String tokenEndpoint = authServerUrl + "/protocol/openid-connect/token";

        return webClient.postAbs(tokenEndpoint)
                .addQueryParam("grant_type", "refresh_token")
                .addQueryParam("client_id", clientId)
                .addQueryParam("refresh_token", refreshToken)
                .send()
                .map(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject body = response.bodyAsJsonObject();
                        return TokenPair.of(
                                body.getString("access_token"),
                                body.getString("refresh_token"),
                                body.getInteger("expires_in", 300)
                        );
                    }

                    Log.warnf("Token exchange failed with status %d", response.statusCode());

                    if (response.statusCode() == 400 || response.statusCode() == 401) {
                        throw new InvalidRefreshTokenException(refreshToken);
                    }

                    throw new RuntimeException(
                            "Keycloak token exchange returned unexpected status: " + response.statusCode()
                    );
                })
                .onFailure()
                .transform(e -> {
                    if (e instanceof InvalidRefreshTokenException) {
                        return e;
                    }
                    Log.errorf(e, "Keycloak token exchange error");
                    return new InvalidRefreshTokenException(refreshToken);
                });
    }

    @Override
    public Uni<Void> revokeRefreshToken(String refreshToken) {
        Log.debugf("Revoking refresh token (hint: %s)", hint(refreshToken));

        String revokeEndpoint = authServerUrl + "/protocol/openid-connect/revoke";

        return webClient.postAbs(revokeEndpoint)
                .addQueryParam("client_id", clientId)
                .addQueryParam("token", refreshToken)
                .addQueryParam("token_type_hint", "refresh_token")
                .send()
                .map(response -> {
                    if (response.statusCode() == 200) {
                        Log.debug("Refresh token revoked successfully");
                    } else {
                        Log.warnf("Token revocation returned status %d (non-fatal)", response.statusCode());
                    }
                    return (Void) null;
                })
                .onFailure()
                .recoverWithNull();
    }

    private static String hint(String token) {
        if (token == null || token.length() < 8) {
            return "****";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
