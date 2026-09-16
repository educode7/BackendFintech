package com.wallet.auth.domain.exception;

/**
 * Thrown when a TOTP code is invalid or expired.
 */
public class InvalidMfaCodeException extends RuntimeException {

    public InvalidMfaCodeException() {
        super("Invalid or expired TOTP code");
    }
}
