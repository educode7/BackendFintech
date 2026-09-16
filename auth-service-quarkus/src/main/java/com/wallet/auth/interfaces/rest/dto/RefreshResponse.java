package com.wallet.auth.interfaces.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RefreshResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") int expiresIn,
        @JsonProperty("token_type") String tokenType
) {
    public static RefreshResponse from(String accessToken, String refreshToken,
                                        int expiresIn) {
        return new RefreshResponse(accessToken, refreshToken, expiresIn, "Bearer");
    }
}
