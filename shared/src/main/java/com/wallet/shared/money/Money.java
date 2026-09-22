package com.wallet.shared.money;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Money as a value object: an immutable amount in a specific ISO-4217 currency.
 * Why BigDecimal: IEEE-754 floats/doubles are unsafe for monetary arithmetic
 * (0.1 + 0.2 != 0.3). BigDecimal keeps decimal precision exact.
 * Equality is by value so two Money instances with same amount and currency compare equal.
 */
public record Money(BigDecimal amount, String currency) {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }
        if (!isIso4217(currency)) {
            throw new IllegalArgumentException("currency must be ISO-4217 (3 uppercase letters): " + currency);
        }
    }

    public static boolean isIso4217(String code) {
        if (code == null || code.length() != 3) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            char c = code.charAt(i);
            if (c < 'A' || c > 'Z') {
                return false;
            }
        }
        try {
            Currency.getInstance(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.signum() < 0) {
            throw new IllegalArgumentException("subtract would produce negative amount");
        }
        return new Money(result, this.currency);
    }

    public boolean isGreaterThanOrEqual(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }

    /** Jackson serialization as "100.50 USD". */
    @JsonValue
    public String json() {
        return amount.toPlainString() + " " + currency;
    }

    @JsonCreator
    public static Money parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("money string is required");
        }
        String[] parts = raw.trim().split("\\s+");
        if (parts.length != 2) {
            throw new IllegalArgumentException("expected 'amount currency', got: " + raw);
        }
        return new Money(new BigDecimal(parts[0]), parts[1]);
    }
}
