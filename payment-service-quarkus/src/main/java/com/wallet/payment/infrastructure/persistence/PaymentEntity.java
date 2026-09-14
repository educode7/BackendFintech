package com.wallet.payment.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import com.wallet.payment.domain.Payment;
import com.wallet.shared.money.Money;

/**
 * JPA entity — the persistence representation of a Payment.
 * <p>
 * This is SEPARATE from the domain Payment aggregate.
 * The domain Payment is a pure POJO; this entity handles ORM mapping.
 * The adapter layer converts between them.
 */
@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payments_idempotency_key", columnNames = "idempotency_key")
})
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "idempotency_key", nullable = false, length = 128, unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public PaymentEntity() {
    }

    /**
     * Convert domain Payment to JPA entity.
     */
    public static PaymentEntity fromDomain(Payment domain) {
        PaymentEntity entity = new PaymentEntity();
        entity.id = UUID.fromString(domain.id());
        entity.userId = domain.userId();
        entity.amount = domain.amount().amount();
        entity.currency = domain.amount().currency();
        entity.status = domain.status().name();
        entity.idempotencyKey = domain.idempotencyKey();
        entity.createdAt = domain.createdAt();
        entity.updatedAt = domain.updatedAt();
        entity.version = domain.version();
        return entity;
    }

    /**
     * Convert JPA entity to domain Payment.
     */
    public Payment toDomain() {
        Money money = new Money(amount, currency);
        Payment.Status domainStatus = Payment.Status.valueOf(status);
        return Payment.of(id.toString(), userId, money, idempotencyKey,
                domainStatus, version, createdAt, updatedAt);
    }

    // --- Getters ---

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
