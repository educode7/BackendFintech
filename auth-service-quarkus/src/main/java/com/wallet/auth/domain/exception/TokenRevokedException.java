package com.wallet.auth.domain.exception;

/**
 * Thrown when attempting to use a previously revoked refresh token.
 */
public class TokenRevokedException extends RuntimeException {

    public TokenRevokedException(String message) {
        super(message);
    }
}
