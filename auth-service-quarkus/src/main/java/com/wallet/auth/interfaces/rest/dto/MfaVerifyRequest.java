package com.wallet.auth.interfaces.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record MfaVerifyRequest(
        @NotNull(message = "MFA code is required")
        @JsonProperty("code") String code
) {}
