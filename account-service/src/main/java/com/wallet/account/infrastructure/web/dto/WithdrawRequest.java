package com.wallet.account.infrastructure.web.dto;

import com.wallet.shared.money.Money;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WithdrawRequest(
    @NotNull @DecimalMin(value = "0.0001") BigDecimal amount,
    @NotBlank String currency
) {
    public Money toMoney() {
        return new Money(amount, currency);
    }
}
