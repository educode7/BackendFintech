package com.wallet.payment.application;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PaymentResponse")
class PaymentResponseTest {

    @Test
    @DisplayName("should create response with all fields")
    void shouldCreate() {
        Instant now = Instant.now();
        PaymentResponse response = new PaymentResponse(
                "pay-001", "user-001", new BigDecimal("25.50"), "USD",
                "COMPLETED", now, now);

        assertEquals("pay-001", response.id());
        assertEquals("user-001", response.userId());
        assertEquals(new BigDecimal("25.50"), response.amount());
        assertEquals("USD", response.currency());
        assertEquals("COMPLETED", response.status());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }

    @Test
    @DisplayName("should handle null optional fields")
    void nullFields() {
        PaymentResponse response = new PaymentResponse(
                "pay-001", "user-001", new BigDecimal("25.50"), "USD",
                "PENDING", null, null);

        assertNull(response.createdAt());
        assertNull(response.updatedAt());
    }
}
