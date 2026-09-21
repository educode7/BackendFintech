package com.wallet.payment.domain;

import java.math.BigDecimal;
import java.util.Objects;

import com.wallet.shared.money.Money;

/**
 * Payment aggregate root.
 * Pure domain object — no framework dependencies (no JPA, no Quarkus, no Redis).
 * <p>
 * State transitions: PENDING → PROCESSING → COMPLETED | FAILED
 * Terminal states: COMPLETED, FAILED — no further transitions allowed.
 */
public final class Payment {

    public enum Status {
        PENDING, PROCESSING, COMPLETED, FAILED;

        public boolean isTerminal() {
            return this == COMPLETED || this == FAILED;
        }
    }

    public enum PaymentType {
        TRANSFER, PAYMENT, REFUND
    }

    public enum PaymentChannel {
        WEB, MOBILE, API, BATCH
    }

    private final String id;
    private final String accountId;
    private final String userId;
    private final Money amount;
    private final String idempotencyKey;
    private final Status status;
    private final long version;
    private final java.time.Instant createdAt;
    private final java.time.Instant updatedAt;

    // Payment type
    private final String paymentType;

    // Beneficiary info
    private final String beneficiaryName;
    private final String beneficiaryDocumentType;
    private final String beneficiaryDocumentNumber;
    private final String beneficiaryAccountNumber;
    private final String beneficiaryBankCode;
    private final String beneficiaryBankName;

    // Sender info (denormalized for audit)
    private final String senderName;
    private final String senderDocumentType;
    private final String senderDocumentNumber;

    // Reference
    private final String reference;
    private final String externalReference;

    // Routing
    private final String channel;
    private final String ipAddress;
    private final String userAgent;

    // Processing
    private final java.time.Instant processedAt;
    private final java.time.Instant failedAt;
    private final String failureReason;
    private final int retryCount;

    // Fees
    private final Money feeAmount;

    private Payment(String id, String accountId, String userId, Money amount, String idempotencyKey,
                    Status status, long version, java.time.Instant createdAt, java.time.Instant updatedAt,
                    String paymentType,
                    String beneficiaryName, String beneficiaryDocumentType, String beneficiaryDocumentNumber,
                    String beneficiaryAccountNumber, String beneficiaryBankCode, String beneficiaryBankName,
                    String senderName, String senderDocumentType, String senderDocumentNumber,
                    String reference, String externalReference,
                    String channel, String ipAddress, String userAgent,
                    java.time.Instant processedAt, java.time.Instant failedAt, String failureReason, int retryCount,
                    Money feeAmount) {
        this.id = Objects.requireNonNull(id, "id");
        this.accountId = Objects.requireNonNull(accountId, "accountId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.status = Objects.requireNonNull(status, "status");
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.paymentType = paymentType;
        this.beneficiaryName = beneficiaryName;
        this.beneficiaryDocumentType = beneficiaryDocumentType;
        this.beneficiaryDocumentNumber = beneficiaryDocumentNumber;
        this.beneficiaryAccountNumber = beneficiaryAccountNumber;
        this.beneficiaryBankCode = beneficiaryBankCode;
        this.beneficiaryBankName = beneficiaryBankName;
        this.senderName = senderName;
        this.senderDocumentType = senderDocumentType;
        this.senderDocumentNumber = senderDocumentNumber;
        this.reference = reference;
        this.externalReference = externalReference;
        this.channel = channel;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.processedAt = processedAt;
        this.failedAt = failedAt;
        this.failureReason = failureReason;
        this.retryCount = retryCount;
        this.feeAmount = feeAmount;
    }

    /**
     * Factory: create a new payment in PENDING state.
     */
    public static Payment create(String id, String accountId, String userId, Money amount, String idempotencyKey) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");

        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        java.time.Instant now = java.time.Instant.now();
        return new Payment(id, accountId, userId, amount, idempotencyKey, Status.PENDING, 0, now, now,
                null, null, null, null, null, null, null,
                null, null, null,
                null, null,
                null, null, null,
                null, null, null, 0,
                null);
    }

    /**
     * Factory: create a new payment with extended fields.
     */
    public static Payment create(String id, String accountId, String userId, Money amount, String idempotencyKey,
                                 String paymentType,
                                 String beneficiaryName, String beneficiaryDocumentType, String beneficiaryDocumentNumber,
                                 String beneficiaryAccountNumber, String beneficiaryBankCode, String beneficiaryBankName,
                                 String senderName, String senderDocumentType, String senderDocumentNumber,
                                 String reference, String externalReference,
                                 String channel, String ipAddress, String userAgent,
                                 Money feeAmount) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");

        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        java.time.Instant now = java.time.Instant.now();
        return new Payment(id, accountId, userId, amount, idempotencyKey, Status.PENDING, 0, now, now,
                paymentType,
                beneficiaryName, beneficiaryDocumentType, beneficiaryDocumentNumber,
                beneficiaryAccountNumber, beneficiaryBankCode, beneficiaryBankName,
                senderName, senderDocumentType, senderDocumentNumber,
                reference, externalReference,
                channel, ipAddress, userAgent,
                null, null, null, 0,
                feeAmount);
    }

    /**
     * Factory: reconstitute from persistence (event store or JPA).
     */
    public static Payment of(String id, String accountId, String userId, Money amount, String idempotencyKey,
                             Status status, long version, java.time.Instant createdAt, java.time.Instant updatedAt) {
        return new Payment(id, accountId, userId, amount, idempotencyKey, status, version, createdAt, updatedAt,
                null, null, null, null, null, null, null,
                null, null, null,
                null, null,
                null, null, null,
                null, null, null, 0,
                null);
    }

    /**
     * Factory: reconstitute from persistence with all fields.
     */
    public static Payment of(String id, String accountId, String userId, Money amount, String idempotencyKey,
                             Status status, long version, java.time.Instant createdAt, java.time.Instant updatedAt,
                             String paymentType,
                             String beneficiaryName, String beneficiaryDocumentType, String beneficiaryDocumentNumber,
                             String beneficiaryAccountNumber, String beneficiaryBankCode, String beneficiaryBankName,
                             String senderName, String senderDocumentType, String senderDocumentNumber,
                             String reference, String externalReference,
                             String channel, String ipAddress, String userAgent,
                             java.time.Instant processedAt, java.time.Instant failedAt, String failureReason, int retryCount,
                             Money feeAmount) {
        return new Payment(id, accountId, userId, amount, idempotencyKey, status, version, createdAt, updatedAt,
                paymentType,
                beneficiaryName, beneficiaryDocumentType, beneficiaryDocumentNumber,
                beneficiaryAccountNumber, beneficiaryBankCode, beneficiaryBankName,
                senderName, senderDocumentType, senderDocumentNumber,
                reference, externalReference,
                channel, ipAddress, userAgent,
                processedAt, failedAt, failureReason, retryCount,
                feeAmount);
    }

    /**
     * Transition to PROCESSING.
     *
     * @return new Payment instance with updated status (immutable)
     */
    public Payment startProcessing() {
        assertNotTerminal();
        assertStatus(Status.PENDING, "PROCESSING");
        return new Payment(id, accountId, userId, amount, idempotencyKey, Status.PROCESSING,
                version + 1, createdAt, java.time.Instant.now(),
                paymentType,
                beneficiaryName, beneficiaryDocumentType, beneficiaryDocumentNumber,
                beneficiaryAccountNumber, beneficiaryBankCode, beneficiaryBankName,
                senderName, senderDocumentType, senderDocumentNumber,
                reference, externalReference,
                channel, ipAddress, userAgent,
                java.time.Instant.now(), null, null, retryCount,
                feeAmount);
    }

    /**
     * Transition to COMPLETED.
     *
     * @return new Payment instance with updated status (immutable)
     */
    public Payment complete() {
        assertNotTerminal();
        assertStatus(Status.PROCESSING, "COMPLETED");
        return new Payment(id, accountId, userId, amount, idempotencyKey, Status.COMPLETED,
                version + 1, createdAt, java.time.Instant.now(),
                paymentType,
                beneficiaryName, beneficiaryDocumentType, beneficiaryDocumentNumber,
                beneficiaryAccountNumber, beneficiaryBankCode, beneficiaryBankName,
                senderName, senderDocumentType, senderDocumentNumber,
                reference, externalReference,
                channel, ipAddress, userAgent,
                java.time.Instant.now(), null, null, retryCount,
                feeAmount);
    }

    /**
     * Transition to FAILED.
     *
     * @return new Payment instance with updated status (immutable)
     */
    public Payment fail() {
        assertNotTerminal();
        assertStatus(Status.PROCESSING, "FAILED");
        return new Payment(id, accountId, userId, amount, idempotencyKey, Status.FAILED,
                version + 1, createdAt, java.time.Instant.now(),
                paymentType,
                beneficiaryName, beneficiaryDocumentType, beneficiaryDocumentNumber,
                beneficiaryAccountNumber, beneficiaryBankCode, beneficiaryBankName,
                senderName, senderDocumentType, senderDocumentNumber,
                reference, externalReference,
                channel, ipAddress, userAgent,
                null, java.time.Instant.now(), null, retryCount + 1,
                feeAmount);
    }

    private void assertNotTerminal() {
        if (status.isTerminal()) {
            throw new IllegalStateException(
                    "Cannot transition from terminal state " + status);
        }
    }

    private void assertStatus(Status expected, String target) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    "Cannot transition from " + this.status + " to " + target);
        }
    }

    // --- Getters ---

    public String id() { return id; }
    public String accountId() { return accountId; }
    public String userId() { return userId; }
    public Money amount() { return amount; }
    public String idempotencyKey() { return idempotencyKey; }
    public Status status() { return status; }
    public long version() { return version; }
    public java.time.Instant createdAt() { return createdAt; }
    public java.time.Instant updatedAt() { return updatedAt; }

    public String paymentType() { return paymentType; }
    public String beneficiaryName() { return beneficiaryName; }
    public String beneficiaryDocumentType() { return beneficiaryDocumentType; }
    public String beneficiaryDocumentNumber() { return beneficiaryDocumentNumber; }
    public String beneficiaryAccountNumber() { return beneficiaryAccountNumber; }
    public String beneficiaryBankCode() { return beneficiaryBankCode; }
    public String beneficiaryBankName() { return beneficiaryBankName; }
    public String senderName() { return senderName; }
    public String senderDocumentType() { return senderDocumentType; }
    public String senderDocumentNumber() { return senderDocumentNumber; }
    public String reference() { return reference; }
    public String externalReference() { return externalReference; }
    public String channel() { return channel; }
    public String ipAddress() { return ipAddress; }
    public String userAgent() { return userAgent; }
    public java.time.Instant processedAt() { return processedAt; }
    public java.time.Instant failedAt() { return failedAt; }
    public String failureReason() { return failureReason; }
    public int retryCount() { return retryCount; }
    public Money feeAmount() { return feeAmount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment p)) return false;
        return id.equals(p.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Payment[id=%s, accountId=%s, userId=%s, amount=%s, status=%s, version=%d]"
                .formatted(id, accountId, userId, amount, status, version);
    }
}
