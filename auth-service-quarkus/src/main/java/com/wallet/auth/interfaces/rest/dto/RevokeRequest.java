package com.wallet.auth.interfaces.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RevokeRequest(
        @JsonProperty("refresh_token") String refreshToken
) {}
