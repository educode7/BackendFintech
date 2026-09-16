package com.wallet.auth.domain.exception;

/**
 * Thrown when a refresh token is invalid or not recognized by Keycloak.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    private final String tokenHint;

    public InvalidRefreshTokenException(String token) {
        super("Invalid refresh token");
        this.tokenHint = mask(token);
    }

    public String tokenHint() {
        return tokenHint;
    }

    private static String mask(String token) {
        if (token == null || token.length() < 8) {
            return "****";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
