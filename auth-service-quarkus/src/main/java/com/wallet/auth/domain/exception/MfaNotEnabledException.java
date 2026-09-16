package com.wallet.auth.domain.exception;

/**
 * Thrown when MFA operations are attempted but MFA is not enabled for the user.
 */
public class MfaNotEnabledException extends RuntimeException {

    private final String userId;

    public MfaNotEnabledException(String userId) {
        super("MFA is not enabled for user");
        this.userId = userId;
    }

    public String userId() {
        return userId;
    }
}
