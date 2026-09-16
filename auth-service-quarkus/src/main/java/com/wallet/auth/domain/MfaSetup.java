package com.wallet.auth.domain;

import java.util.List;

/**
 * MFA setup value object — QR code, secret, and recovery codes.
 */
public record MfaSetup(
        String qrCode,
        String secret,
        List<String> recoveryCodes,
        String issuer,
        String accountName
) {}
