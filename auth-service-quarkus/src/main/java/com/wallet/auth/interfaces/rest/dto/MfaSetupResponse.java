package com.wallet.auth.interfaces.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MfaSetupResponse(
        @JsonProperty("qr_code") String qrCode,
        @JsonProperty("secret") String secret,
        @JsonProperty("recovery_codes") List<String> recoveryCodes,
        @JsonProperty("issuer") String issuer,
        @JsonProperty("account_name") String accountName
) {}
