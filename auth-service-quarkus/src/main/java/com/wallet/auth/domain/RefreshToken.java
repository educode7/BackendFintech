package com.wallet.auth.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Refresh token entity — immutable domain model with factory and state methods.
 */
public final class RefreshToken {

    private final String id;
    private final String userId;
    private final String token;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final boolean revoked;

    private RefreshToken(String id, String userId, String token,
                         Instant createdAt, Instant expiresAt, boolean revoked) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.token = Objects.requireNonNull(token);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.revoked = revoked;
    }

    public static RefreshToken create(String userId, String token, Instant expiresAt) {
        return new RefreshToken(
                UUID.randomUUID().toString(),
                userId,
                token,
                Instant.now(),
                expiresAt,
                false
        );
    }

    public RefreshToken revoke() {
        if (revoked) {
            throw new IllegalStateException("Token is already revoked");
        }
        return new RefreshToken(id, userId, token, createdAt, Instant.now(), true);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revoked;
    }

    public String id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public String token() {
        return token;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }
}
