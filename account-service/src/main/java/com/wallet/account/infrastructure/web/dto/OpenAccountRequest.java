package com.wallet.account.infrastructure.web.dto;

import com.wallet.shared.money.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OpenAccountRequest(
    @NotBlank @Size(max = 64) String userId,
    @NotNull Money initialBalance
) { }
