package com.wallet.account.application;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AccountResponse")
class AccountResponseTest {

    @Test
    @DisplayName("should create response with all fields")
    void shouldCreate() {
        Instant now = Instant.now();
        AccountResponse response = new AccountResponse(
                "acc-001", "user-001", new BigDecimal("100.00"), "USD",
                "OPEN", 1, now,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null);

        assertEquals("acc-001", response.accountId());
        assertEquals("user-001", response.userId());
        assertEquals(new BigDecimal("100.00"), response.balanceAmount());
        assertEquals("USD", response.balanceCurrency());
        assertEquals("OPEN", response.status());
        assertEquals(1, response.version());
        assertEquals(now, response.lastUpdated());
    }

    @Test
    @DisplayName("should handle null fields")
    void nullFields() {
        AccountResponse response = new AccountResponse(
                "acc-001", "user-001", BigDecimal.ZERO, "USD",
                "OPEN", 0, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null);

        assertNull(response.lastUpdated());
    }
}
