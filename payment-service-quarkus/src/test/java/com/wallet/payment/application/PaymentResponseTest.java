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
                "pay-001", "acc-001", "user-001", new BigDecimal("25.50"), "USD",
                "COMPLETED", now, now,
                // Payment type
                "TRANSFER",
                // Beneficiary info
                "John Doe", "DNI", "12345678", "987654321", "001", "Banco Nacional",
                // Sender info
                "Jane Doe", "DNI", "87654321",
                // Reference
                "INV-001", "EXT-001",
                // Routing
                "WEB",
                // Processing
                now, null, null, 0,
                // Fees
                new BigDecimal("1.50"), "USD");

        assertEquals("pay-001", response.id());
        assertEquals("acc-001", response.accountId());
        assertEquals("user-001", response.userId());
        assertEquals(new BigDecimal("25.50"), response.amount());
        assertEquals("USD", response.currency());
        assertEquals("COMPLETED", response.status());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
        assertEquals("TRANSFER", response.paymentType());
        assertEquals("John Doe", response.beneficiaryName());
        assertEquals("DNI", response.beneficiaryDocumentType());
        assertEquals("12345678", response.beneficiaryDocumentNumber());
        assertEquals("987654321", response.beneficiaryAccountNumber());
        assertEquals("001", response.beneficiaryBankCode());
        assertEquals("Banco Nacional", response.beneficiaryBankName());
        assertEquals("Jane Doe", response.senderName());
        assertEquals("DNI", response.senderDocumentType());
        assertEquals("87654321", response.senderDocumentNumber());
        assertEquals("INV-001", response.reference());
        assertEquals("EXT-001", response.externalReference());
        assertEquals("WEB", response.channel());
        assertEquals(now, response.processedAt());
        assertNull(response.failedAt());
        assertNull(response.failureReason());
        assertEquals(0, response.retryCount());
        assertEquals(new BigDecimal("1.50"), response.feeAmount());
        assertEquals("USD", response.feeCurrency());
    }

    @Test
    @DisplayName("should handle null optional fields")
    void nullFields() {
        PaymentResponse response = new PaymentResponse(
                "pay-001", "acc-001", "user-001", new BigDecimal("25.50"), "USD",
                "PENDING", null, null,
                null, null, null, null, null, null, null,
                null, null, null,
                null, null,
                null,
                null, null, null, 0,
                null, null);

        assertEquals("acc-001", response.accountId());
        assertNull(response.createdAt());
        assertNull(response.updatedAt());
    }
}
