package com.wallet.auth.domain;

/**
 * MFA verification result value object.
 */
public record MfaVerification(
        boolean verified,
        int backupCodesRemaining
) {}
