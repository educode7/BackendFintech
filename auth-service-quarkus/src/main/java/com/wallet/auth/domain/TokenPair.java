package com.wallet.auth.domain;

/**
 * Token pair value object — returned after successful token exchange.
 */
public record TokenPair(
        String accessToken,
        String refreshToken,
        int expiresIn
) {
    public static TokenPair of(String access, String refresh, int expires) {
        return new TokenPair(access, refresh, expires);
    }
}
