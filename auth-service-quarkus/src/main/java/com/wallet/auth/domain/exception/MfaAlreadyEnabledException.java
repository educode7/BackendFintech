package com.wallet.auth.domain.exception;

/**
 * Thrown when MFA setup is requested but MFA is already enabled for the user.
 */
public class MfaAlreadyEnabledException extends RuntimeException {

    private final String userId;

    public MfaAlreadyEnabledException(String userId) {
        super("MFA is already enabled for user");
        this.userId = userId;
    }

    public String userId() {
        return userId;
    }
}
