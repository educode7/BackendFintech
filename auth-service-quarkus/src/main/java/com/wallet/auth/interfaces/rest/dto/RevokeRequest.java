package com.wallet.auth.interfaces.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Revoke request body — optional when refresh_token is in HttpOnly cookie.
 * The cookie takes priority; this body is backward compatibility during migration.
 */
public record RevokeRequest(
        @JsonProperty("refresh_token") String refreshToken
) {}
