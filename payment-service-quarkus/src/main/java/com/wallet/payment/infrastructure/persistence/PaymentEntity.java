package com.wallet.payment.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
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

    @Column(name = "account_id", nullable = false, length = 64)
    private String accountId;

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

    // Payment type
    @Column(name = "payment_type", length = 30)
    private String paymentType;

    // Beneficiary info
    @Column(name = "beneficiary_name", length = 120)
    private String beneficiaryName;

    @Column(name = "beneficiary_document_type", length = 30)
    private String beneficiaryDocumentType;

    @Column(name = "beneficiary_document_number", length = 20)
    private String beneficiaryDocumentNumber;

    @Column(name = "beneficiary_account_number", length = 20)
    private String beneficiaryAccountNumber;

    @Column(name = "beneficiary_bank_code", length = 10)
    private String beneficiaryBankCode;

    @Column(name = "beneficiary_bank_name", length = 100)
    private String beneficiaryBankName;

    // Sender info
    @Column(name = "sender_name", length = 120)
    private String senderName;

    @Column(name = "sender_document_type", length = 30)
    private String senderDocumentType;

    @Column(name = "sender_document_number", length = 20)
    private String senderDocumentNumber;

    // Reference
    @Column(name = "reference", length = 255)
    private String reference;

    @Column(name = "external_reference", length = 255)
    private String externalReference;

    // Routing
    @Column(name = "channel", length = 20)
    private String channel;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // Processing
    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "retry_count")
    private int retryCount;

    // Fees
    @Column(name = "fee_amount", precision = 19, scale = 4)
    private BigDecimal feeAmount;

    @Column(name = "fee_currency", length = 3)
    private String feeCurrency;

    public PaymentEntity() {
    }

    /**
     * Convert domain Payment to JPA entity for NEW persistence.
     * Does NOT set the ID — Hibernate generates it via @GeneratedValue.
     * After persist(), copy the generated ID back to the domain via toDomain().
     */
    public static PaymentEntity fromDomainNew(Payment domain) {
        PaymentEntity entity = new PaymentEntity();
        // ID is NOT set — Hibernate generates UUID via @GeneratedValue
        entity.userId = domain.userId();
        entity.accountId = domain.accountId();
        entity.amount = domain.amount().amount();
        entity.currency = domain.amount().currency();
        entity.status = domain.status().name();
        entity.idempotencyKey = domain.idempotencyKey();
        entity.createdAt = domain.createdAt();
        entity.updatedAt = domain.updatedAt();
        // Extended fields
        entity.paymentType = domain.paymentType();
        entity.beneficiaryName = domain.beneficiaryName();
        entity.beneficiaryDocumentType = domain.beneficiaryDocumentType();
        entity.beneficiaryDocumentNumber = domain.beneficiaryDocumentNumber();
        entity.beneficiaryAccountNumber = domain.beneficiaryAccountNumber();
        entity.beneficiaryBankCode = domain.beneficiaryBankCode();
        entity.beneficiaryBankName = domain.beneficiaryBankName();
        entity.senderName = domain.senderName();
        entity.senderDocumentType = domain.senderDocumentType();
        entity.senderDocumentNumber = domain.senderDocumentNumber();
        entity.reference = domain.reference();
        entity.externalReference = domain.externalReference();
        entity.channel = domain.channel();
        entity.ipAddress = domain.ipAddress();
        entity.userAgent = domain.userAgent();
        entity.processedAt = domain.processedAt();
        entity.failedAt = domain.failedAt();
        entity.failureReason = domain.failureReason();
        entity.retryCount = domain.retryCount();
        if (domain.feeAmount() != null) {
            entity.feeAmount = domain.feeAmount().amount();
            entity.feeCurrency = domain.feeAmount().currency();
        }
        // version is NOT set — Hibernate initializes to 0 on INSERT
        return entity;
    }

    /**
     * Convert domain Payment to JPA entity for MERGE (update existing).
     * Sets the ID and version from the domain for optimistic locking.
     */
    public static PaymentEntity fromDomain(Payment domain) {
        PaymentEntity entity = new PaymentEntity();
        entity.id = UUID.fromString(domain.id());
        entity.userId = domain.userId();
        entity.accountId = domain.accountId();
        entity.amount = domain.amount().amount();
        entity.currency = domain.amount().currency();
        entity.status = domain.status().name();
        entity.idempotencyKey = domain.idempotencyKey();
        entity.createdAt = domain.createdAt();
        entity.updatedAt = domain.updatedAt();
        entity.version = domain.version();
        // Extended fields
        entity.paymentType = domain.paymentType();
        entity.beneficiaryName = domain.beneficiaryName();
        entity.beneficiaryDocumentType = domain.beneficiaryDocumentType();
        entity.beneficiaryDocumentNumber = domain.beneficiaryDocumentNumber();
        entity.beneficiaryAccountNumber = domain.beneficiaryAccountNumber();
        entity.beneficiaryBankCode = domain.beneficiaryBankCode();
        entity.beneficiaryBankName = domain.beneficiaryBankName();
        entity.senderName = domain.senderName();
        entity.senderDocumentType = domain.senderDocumentType();
        entity.senderDocumentNumber = domain.senderDocumentNumber();
        entity.reference = domain.reference();
        entity.externalReference = domain.externalReference();
        entity.channel = domain.channel();
        entity.ipAddress = domain.ipAddress();
        entity.userAgent = domain.userAgent();
        entity.processedAt = domain.processedAt();
        entity.failedAt = domain.failedAt();
        entity.failureReason = domain.failureReason();
        entity.retryCount = domain.retryCount();
        if (domain.feeAmount() != null) {
            entity.feeAmount = domain.feeAmount().amount();
            entity.feeCurrency = domain.feeAmount().currency();
        }
        return entity;
    }

    /**
     * Convert JPA entity to domain Payment.
     */
    public Payment toDomain() {
        Money money = new Money(amount, currency);
        Payment.Status domainStatus = Payment.Status.valueOf(status);
        Money feeMoney = (feeAmount != null && feeCurrency != null) ? new Money(feeAmount, feeCurrency) : null;
        return Payment.of(id.toString(), accountId, userId, money, idempotencyKey,
                domainStatus, version, createdAt, updatedAt,
                paymentType,
                beneficiaryName, beneficiaryDocumentType, beneficiaryDocumentNumber,
                beneficiaryAccountNumber, beneficiaryBankCode, beneficiaryBankName,
                senderName, senderDocumentType, senderDocumentNumber,
                reference, externalReference,
                channel, ipAddress, userAgent,
                processedAt, failedAt, failureReason, retryCount,
                feeMoney);
    }

    // --- Getters ---

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public String getAccountId() { return accountId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
    public String getPaymentType() { return paymentType; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public String getBeneficiaryDocumentType() { return beneficiaryDocumentType; }
    public String getBeneficiaryDocumentNumber() { return beneficiaryDocumentNumber; }
    public String getBeneficiaryAccountNumber() { return beneficiaryAccountNumber; }
    public String getBeneficiaryBankCode() { return beneficiaryBankCode; }
    public String getBeneficiaryBankName() { return beneficiaryBankName; }
    public String getSenderName() { return senderName; }
    public String getSenderDocumentType() { return senderDocumentType; }
    public String getSenderDocumentNumber() { return senderDocumentNumber; }
    public String getReference() { return reference; }
    public String getExternalReference() { return externalReference; }
    public String getChannel() { return channel; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public Instant getProcessedAt() { return processedAt; }
    public Instant getFailedAt() { return failedAt; }
    public String getFailureReason() { return failureReason; }
    public int getRetryCount() { return retryCount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public String getFeeCurrency() { return feeCurrency; }
}
