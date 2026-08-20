package com.wallet.payment.domain;

import com.wallet.shared.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.EntityListeners;

/**
 * Payment aggregate root persisted as a JPA entity.
 *
 * Why a flat row and not event-sourced:
 * The brief explicitly asks account-service to be event-sourced, but payment
 * remains a regular CRUD aggregate — the only invariant the payment owns is
 * "did this idempotency key already succeed?" which a unique index handles cleanly.
 *
 * Money is split into amount + currency columns (NUMERIC + CHAR(3)) because:
 * 1. Indexable, queryable, summable in SQL.
 * 2. Avoids JSONB round-trip for the most frequently read column.
 * 3. We re-hydrate into a {@link Money} value object in the application layer.
 *
 * The {@link Version} column provides JPA optimistic locking on top of the
 * unique idempotency_key constraint for double safety.
 */
@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Payment {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 128, unique = true)
    private String idempotencyKey;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public Money money() {
        return new Money(amount, currency);
    }
}
