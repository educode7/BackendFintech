package com.wallet.auth.interfaces.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MfaDisableRequest(
        @JsonProperty("code") String code
) {}
