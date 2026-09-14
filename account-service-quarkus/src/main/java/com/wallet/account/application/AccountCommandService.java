package com.wallet.account.application;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import com.wallet.account.domain.Account;
import com.wallet.account.domain.AccountView;
import com.wallet.account.domain.AccountViewRepository;
import com.wallet.account.domain.EventPublisher;
import com.wallet.account.domain.EventStore;
import com.wallet.shared.money.Money;

import io.smallrye.mutiny.Uni;

/**
 * Use case: account write side (CQRS command side).
 * <p>
 * Orchestrates: create account → append events → publish to Kafka → update projection.
 */
@Singleton
public class AccountCommandService {

    private static final Logger log = Logger.getLogger(AccountCommandService.class);

    private final EventStore eventStore;
    private final AccountViewRepository viewRepository;
    private final EventPublisher eventPublisher;

    @Inject
    public AccountCommandService(EventStore eventStore,
                                 AccountViewRepository viewRepository,
                                 EventPublisher eventPublisher) {
        this.eventStore = eventStore;
        this.viewRepository = viewRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Open a new account.
     */
    public Uni<AccountResponse> openAccount(AccountCommand.OpenAccount command) {
        String accountId = com.wallet.shared.util.IdGenerator.newId();

        // 1. Create aggregate
        Account account = Account.open(accountId, command.userId(), command.initialBalance());

        // 2. Append events to store
        eventStore.appendEvents(accountId, account.getPendingEvents(), 0);

        log.infof("Account opened: id=%s, userId=%s, balance=%s", accountId, command.userId(), command.initialBalance());

        // 3. Create projection
        AccountView view = AccountView.fromDomain(account);
        viewRepository.save(view);

        // 4. Publish events to Kafka
        publishEvents(account, command.requestId());

        // 5. Clear pending events after publishing
        account.clearPendingEvents();

        return Uni.createFrom().item(AccountResponse.from(view));
    }

    /**
     * Deposit funds into an account.
     */
    public Uni<AccountResponse> deposit(AccountCommand.Deposit command) {
        return loadAggregate(command.accountId())
                .onItem().transformToUni(account -> {
                    Money amount = new Money(command.amount(), command.currency());

                    // 1. Apply business logic
                    account.deposit(amount);

                    // 2. Append events
                    eventStore.appendEvents(command.accountId(), account.getPendingEvents(), account.version() - account.getPendingEvents().size());

                    log.infof("Deposit: accountId=%s, amount=%s", command.accountId(), amount);

                    // 3. Update projection
                    AccountView view = AccountView.fromDomain(account);
                    viewRepository.save(view);

                    // 4. Publish events
                    publishEvents(account, command.requestId());

                    // 5. Clear pending events after publishing
                    account.clearPendingEvents();

                    return Uni.createFrom().item(AccountResponse.from(view));
                });
    }

    /**
     * Withdraw funds from an account.
     */
    public Uni<AccountResponse> withdraw(AccountCommand.Withdraw command) {
        return loadAggregate(command.accountId())
                .onItem().transformToUni(account -> {
                    Money amount = new Money(command.amount(), command.currency());

                    // 1. Apply business logic
                    account.withdraw(amount);

                    // 2. Append events
                    eventStore.appendEvents(command.accountId(), account.getPendingEvents(), account.version() - account.getPendingEvents().size());

                    log.infof("Withdrawal: accountId=%s, amount=%s", command.accountId(), amount);

                    // 3. Update projection
                    AccountView view = AccountView.fromDomain(account);
                    viewRepository.save(view);

                    // 4. Publish events
                    publishEvents(account, command.requestId());

                    // 5. Clear pending events after publishing
                    account.clearPendingEvents();

                    return Uni.createFrom().item(AccountResponse.from(view));
                });
    }

    /**
     * Rebuild aggregate from event stream.
     */
    private Uni<Account> loadAggregate(String accountId) {
        List<com.wallet.shared.event.AccountEvent> events = eventStore.loadEvents(accountId);
        if (events.isEmpty()) {
            return Uni.createFrom().failure(
                    new com.wallet.account.domain.exception.AccountNotFoundException(accountId));
        }

        // Rebuild from first event
        com.wallet.shared.event.AccountEvent first = events.getFirst();
        Account account;
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

    private void publishEvents(Account account, String correlationId) {
        for (com.wallet.shared.event.AccountEvent event : account.getPendingEvents()) {
            String eventType = event.getClass().getSimpleName();
            String payload = com.wallet.shared.util.JsonUtil.toJson(event);
            eventPublisher.publish(eventType, account.accountId(), payload, correlationId);
        }
    }
}
