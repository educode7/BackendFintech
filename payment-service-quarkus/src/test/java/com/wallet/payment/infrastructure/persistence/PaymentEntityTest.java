package com.wallet.payment.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wallet.payment.domain.Payment;
import com.wallet.shared.money.Money;

@DisplayName("PaymentEntity mapping")
class PaymentEntityTest {

    @Test
    @DisplayName("should map domain to entity")
    void domainToEntity() {
        String uuid = UUID.randomUUID().toString();
        Payment payment = Payment.create(uuid, "user-001",
                new Money(new BigDecimal("25.50"), "USD"), "key-001");
        PaymentEntity entity = PaymentEntity.fromDomain(payment);

        assertEquals(uuid, entity.getId().toString());
        assertEquals("user-001", entity.getUserId());
        assertEquals(new BigDecimal("25.50"), entity.getAmount());
        assertEquals("USD", entity.getCurrency());
        assertEquals("PENDING", entity.getStatus());
        assertEquals("key-001", entity.getIdempotencyKey());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    @DisplayName("should map entity to domain")
    void entityToDomain() {
        String uuid = UUID.randomUUID().toString();
        Payment payment = Payment.create(uuid, "user-001",
                new Money(new BigDecimal("25.50"), "USD"), "key-001");
        Payment completed = payment.startProcessing().complete();
        PaymentEntity entity = PaymentEntity.fromDomain(completed);

        Payment restored = entity.toDomain();

        assertEquals(uuid, restored.id());
        assertEquals("user-001", restored.userId());
        assertEquals(new BigDecimal("25.50"), restored.amount().amount());
        assertEquals("USD", restored.amount().currency());
        assertEquals(Payment.Status.COMPLETED, restored.status());
    }

    @Test
    @DisplayName("should roundtrip domain -> entity -> domain")
    void roundtrip() {
        String uuid = UUID.randomUUID().toString();
        Payment original = Payment.create(uuid, "user-001",
                new Money(new BigDecimal("25.50"), "USD"), "key-001");
        PaymentEntity entity = PaymentEntity.fromDomain(original);
        Payment restored = entity.toDomain();

        assertEquals(original.id(), restored.id());
        assertEquals(original.userId(), restored.userId());
        assertEquals(original.amount(), restored.amount());
        assertEquals(original.status(), restored.status());
    }
}
