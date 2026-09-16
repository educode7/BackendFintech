package com.wallet.auth.interfaces.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MfaVerifyResponse(
        @JsonProperty("verified") boolean verified,
        @JsonProperty("backup_codes_remaining") int backupCodesRemaining
) {}
