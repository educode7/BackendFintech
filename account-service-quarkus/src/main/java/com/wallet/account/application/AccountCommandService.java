package com.wallet.account.application;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import com.wallet.account.domain.Account;
import com.wallet.account.domain.AccountSnapshot;
import com.wallet.account.domain.AccountView;
import com.wallet.account.domain.AccountViewRepository;
import com.wallet.account.domain.EventStore;
import com.wallet.account.domain.SnapshotStore;
import com.wallet.shared.money.Money;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;

/**
 * Use case: account write side (CQRS command side).
 * <p>
 * Orchestrates: create account → append events → update projection.
 * Events are published to Kafka by the OutboxPoller (transactional outbox pattern).
 */
@Singleton
public class AccountCommandService {

    private static final Logger log = Logger.getLogger(AccountCommandService.class);

    private final EventStore eventStore;
    private final AccountViewRepository viewRepository;
    private final SnapshotStore snapshotStore;

    /** Take a snapshot every N events to bound replay cost. */
    private static final int SNAPSHOT_INTERVAL = 20;

    @Inject
    public AccountCommandService(EventStore eventStore,
                                 AccountViewRepository viewRepository,
                                 SnapshotStore snapshotStore) {
        this.eventStore = eventStore;
        this.viewRepository = viewRepository;
        this.snapshotStore = snapshotStore;
    }

    /**
     * Open a new account.
     */
    @WithSpan("open-account")
    public Uni<AccountResponse> openAccount(@SpanAttribute("account.user_id") AccountCommand.OpenAccount command) {
        String accountId = com.wallet.shared.util.IdGenerator.newId();

        // 1. Create aggregate
        Account account = Account.open(accountId, command.userId(), command.initialBalance());

        // 2. Append events to store (outbox — published by OutboxPoller)
        eventStore.appendEvents(accountId, account.getPendingEvents(), 0);

        log.infof("Account opened: id=%s, userId=%s, balance=%s", accountId, command.userId(), command.initialBalance());

        // 3. Create projection
        AccountView view = AccountView.fromDomain(account);
        viewRepository.save(view);

        // 4. Clear pending events after persisting
        account.clearPendingEvents();

        return Uni.createFrom().item(AccountResponse.from(view));
    }

    /**
     * Deposit funds into an account.
     */
    @WithSpan("deposit")
    public Uni<AccountResponse> deposit(@SpanAttribute("account.id") AccountCommand.Deposit command) {
        return loadAggregate(command.accountId())
                .onItem().transformToUni(account -> {
                    Money amount = new Money(command.amount(), command.currency());

                    // 1. Apply business logic
                    account.deposit(amount);

                    // 2. Append events (outbox — published by OutboxPoller)
                    eventStore.appendEvents(command.accountId(), account.getPendingEvents(), account.version() - account.getPendingEvents().size());

                    log.infof("Deposit: accountId=%s, amount=%s", command.accountId(), amount);

                    // 3. Update projection
                    AccountView view = AccountView.fromDomain(account);
                    viewRepository.save(view);

                    // 4. Snapshot if interval reached
                    saveSnapshotIfNeeded(account);

                    // 5. Clear pending events after persisting
                    account.clearPendingEvents();

                    return Uni.createFrom().item(AccountResponse.from(view));
                });
    }

    /**
     * Withdraw funds from an account.
     */
    @WithSpan("withdraw")
    public Uni<AccountResponse> withdraw(@SpanAttribute("account.id") AccountCommand.Withdraw command) {
        return loadAggregate(command.accountId())
                .onItem().transformToUni(account -> {
                    Money amount = new Money(command.amount(), command.currency());

                    // 1. Apply business logic
                    account.withdraw(amount);

                    // 2. Append events (outbox — published by OutboxPoller)
                    eventStore.appendEvents(command.accountId(), account.getPendingEvents(), account.version() - account.getPendingEvents().size());

                    log.infof("Withdrawal: accountId=%s, amount=%s", command.accountId(), amount);

                    // 3. Update projection
                    AccountView view = AccountView.fromDomain(account);
                    viewRepository.save(view);

                    // 4. Snapshot if interval reached
                    saveSnapshotIfNeeded(account);

                    // 5. Clear pending events after persisting
                    account.clearPendingEvents();

                    return Uni.createFrom().item(AccountResponse.from(view));
                });
    }

    /**
     * Rebuild aggregate from snapshot + incremental events.
     * <p>
     * If a snapshot exists, only events after the snapshot version are replayed — O(N) where N
     * is events-since-snapshot rather than total lifetime events.
     */
    private Uni<Account> loadAggregate(String accountId) {
        // 1. Try snapshot first
        java.util.Optional<AccountSnapshot> snapshotOpt = snapshotStore.findLatest(accountId);

        List<com.wallet.shared.event.AccountEvent> events;
        Account account;

        if (snapshotOpt.isPresent()) {
            AccountSnapshot snap = snapshotOpt.get();
            account = Account.reconstitute(snap.accountId(), snap.userId(),
                    snap.balance(), Account.Status.valueOf(snap.status()), snap.version());

            // Only replay events after the snapshot version
            events = eventStore.loadEventsAfter(accountId, snap.version());
        } else {
            // No snapshot — load all events from the beginning
            events = eventStore.loadEvents(accountId);
            if (events.isEmpty()) {
                return Uni.createFrom().failure(
                        new com.wallet.account.domain.exception.AccountNotFoundException(accountId));
            }

            // Rebuild from first event
            com.wallet.shared.event.AccountEvent first = events.getFirst();
            if (first instanceof com.wallet.shared.event.AccountOpenedEvent opened) {
                account = Account.open(opened.accountId(), opened.userId(), opened.initialBalance());
                account.clearPendingEvents();
            } else {
                return Uni.createFrom().failure(
                        new IllegalStateException("First event must be AccountOpenedEvent"));
            }

            // Replay remaining events
            for (int i = 1; i < events.size(); i++) {
                account.apply(events.get(i));
            }

            return Uni.createFrom().item(account);
        }

        // Replay incremental events on top of snapshot
        for (com.wallet.shared.event.AccountEvent event : events) {
            account.apply(event);
        }

        return Uni.createFrom().item(account);
    }

    /**
     * Persist a snapshot if the aggregate has crossed the snapshot interval.
     */
    private void saveSnapshotIfNeeded(Account account) {
        if (account.version() > 0 && account.version() % SNAPSHOT_INTERVAL == 0) {
            snapshotStore.save(AccountSnapshot.from(account));
        }
    }
}
