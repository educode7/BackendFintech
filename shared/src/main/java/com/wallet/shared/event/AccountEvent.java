package com.wallet.shared.event;

/**
 * Sealed hierarchy of all account-aggregate domain events.
 *
 * Why sealed: the compiler enforces exhaustive pattern matching, so we cannot
 * forget to handle a new event variant in {@code EventApplier} or the projection
 * service — the build will fail.
 *
 * Lives in {@code shared} (not {@code account-service}) because:
 *  - Other services may eventually emit/consume these (e.g. notification-service
 *    subscribing to MoneyDepositedEvent).
 *  - The wire format on Kafka IS the event payload, so the contract must be
 *    visible to both producers and consumers.
 */
public sealed interface AccountEvent
    permits AccountOpenedEvent, MoneyDepositedEvent, MoneyWithdrawnEvent {

    String accountId();

    EventMetadata metadata();
}
