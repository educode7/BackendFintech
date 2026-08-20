package com.wallet.account.application;

import com.wallet.account.domain.Account;
import com.wallet.account.domain.exception.AccountNotFoundException;
import com.wallet.account.infrastructure.persistence.JpaEventStore;
import com.wallet.account.infrastructure.projection.AccountView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Read side of the CQRS split.
 *
 * Resolution order:
 *  1. Replay events from the event store (always correct, always available).
 *
 * Why not query Redis here:
 * - The projection lives in Redis for HOT reads at the controller layer.
 * - This service is for cases that need authoritative state (e.g. audit).
 * - Keeping responsibilities small lets us swap caches without touching domain code.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountQueryService {

    private final JpaEventStore eventStore;

    public Account findAggregate(String accountId) {
        return eventStore.load(accountId);
    }

    public AccountView findView(String accountId) {
        Account account = eventStore.load(accountId);
        return AccountView.fromDomain(account);
    }
}
