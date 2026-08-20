package com.wallet.shared.kafka;

/**
 * Centralised Kafka topic and consumer-group constants.
 * Why centralise: one typo in a topic name and consumers silently see zero messages.
 */
public final class KafkaTopics {

    private KafkaTopics() { }

    // Topics — kept short, single-segment, no version suffix on purpose for L3 simplicity.
    public static final String PAYMENT_EVENTS       = "payment.events";
    public static final String ACCOUNT_EVENTS       = "account.events";
    public static final String NOTIFICATION_EVENTS  = "notification.events";

    // Consumer groups — every service that subscribes gets its own group so each
    // receives every event (no fan-out stealing).
    public static final String GROUP_ACCOUNT        = "account-service";
    public static final String GROUP_NOTIFICATION   = "notification-service";

    // Headers
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    public static final String HEADER_EVENT_TYPE     = "event-type";
    public static final String HEADER_EVENT_ID       = "event-id";
}
