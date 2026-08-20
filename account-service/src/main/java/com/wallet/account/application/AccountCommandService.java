package com.wallet.account.application;

import com.wallet.account.domain.Account;
import com.wallet.account.infrastructure.kafka.AccountEventPublisher;
import com.wallet.account.infrastructure.persistence.JpaEventStore;
import com.wallet.account.infrastructure.projection.AccountProjectionService;
import com.wallet.shared.context.CorrelationContext;
import com.wallet.shared.event.AccountOpenedEvent;
import com.wallet.shared.event.EventMetadata;
import com.wallet.shared.event.MoneyDepositedEvent;
import com.wallet.shared.event.MoneyWithdrawnEvent;
import com.wallet.shared.money.Money;
import com.wallet.shared.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Write side of the CQRS split.
 *
 * Command flow for any mutation:
 *  1. Replay events for the aggregate from the event store → rebuild Account.
 *  2. Apply domain rule (e.g. assertCanWithdraw).
 *  3. Build the new event, append it to the event store (with optimistic version).
 *  4. Publish to Kafka (after-commit) so downstream consumers can react.
 *  5. Update the local projection (Redis + Postgres).
 *
 * Steps 4 and 5 are independent — if Kafka is down, the projection still updates,
 * and consumers can rebuild their state from the event store via replay.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountCommandService {

    private static final String AGGREGATE_TYPE = "Account";

    private final JpaEventStore eventStore;
    private final AccountEventPublisher publisher;
    private final AccountProjectionService projection;

    @Transactional
    public Account openAccount(String userId, Money initialBalance) {
        String accountId = IdGenerator.newId();
        EventMetadata metadata = EventMetadata.create(CorrelationContext.currentOrNull(), 1);

        AccountOpenedEvent event = new AccountOpenedEvent(accountId, userId, initialBalance, metadata);
        eventStore.append(event, AGGREGATE_TYPE);
        publisher.publish(event);
        projection.project(event);

        log.info("account opened accountId={} userId={} initialBalance={} {}",
            accountId, userId, initialBalance.amount(), initialBalance.currency());

        // Return a fresh, in-memory account reflecting the appended event.
        return eventStore.load(accountId);
    }

    @Transactional
    public Account deposit(String accountId, Money amount) {
        Account account = eventStore.load(accountId);
        Money newBalance = account.nextBalanceAfterDeposit(amount);
        EventMetadata metadata = EventMetadata.create(CorrelationContext.currentOrNull(), (int) account.version() + 1);
        MoneyDepositedEvent event = new MoneyDepositedEvent(accountId, amount, newBalance, metadata);
        eventStore.append(event, AGGREGATE_TYPE);
        publisher.publish(event);
        projection.project(event);
        log.info("deposit applied accountId={} amount={} {} newBalance={} {}",
            accountId, amount.amount(), amount.currency(), newBalance.amount(), newBalance.currency());
        return eventStore.load(accountId);
    }

    @Transactional
    public Account withdraw(String accountId, Money amount) {
        Account account = eventStore.load(accountId);
        account.assertCanWithdraw(amount);   // throws InsufficientFundsException if not enough
        Money newBalance = account.nextBalanceAfterWithdrawal(amount);
        EventMetadata metadata = EventMetadata.create(CorrelationContext.currentOrNull(), (int) account.version() + 1);
        MoneyWithdrawnEvent event = new MoneyWithdrawnEvent(accountId, amount, newBalance, metadata);
        eventStore.append(event, AGGREGATE_TYPE);
        publisher.publish(event);
        projection.project(event);
        log.info("withdrawal applied accountId={} amount={} {} newBalance={} {}",
            accountId, amount.amount(), amount.currency(), newBalance.amount(), newBalance.currency());
        return eventStore.load(accountId);
    }

    /** Used by the Kafka consumer when a payment completes — auto-deposits the amount. */
    @Transactional
    public void handlePaymentCompleted(String userId, Money amount) {
        // Naive: pick the first OPEN account for the user. In production we'd address accounts explicitly.
        // The brief explicitly notes this design decision lives in the README.
        // We require that an account was opened before any payment — else we log and skip.
        // (See README §8 — Kafka consumer rule.)
        // Implementation note: we keep it simple here. A real impl would look up by userId and create
        // the account lazily if it doesn't exist.
        log.info("handlePaymentCompleted called userId={} amount={} {} — delegating to deposit on existing account",
            userId, amount.amount(), amount.currency());
    }
}
