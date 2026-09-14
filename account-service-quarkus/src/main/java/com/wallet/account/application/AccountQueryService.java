package com.wallet.account.application;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.wallet.account.domain.AccountView;
import com.wallet.account.domain.AccountViewRepository;
import com.wallet.account.domain.exception.AccountNotFoundException;

import io.smallrye.mutiny.Uni;

/**
 * Use case: account read side (CQRS query side).
 */
@Singleton
public class AccountQueryService {

    private final AccountViewRepository viewRepository;

    @Inject
    public AccountQueryService(AccountViewRepository viewRepository) {
        this.viewRepository = viewRepository;
    }

    /**
     * Find an account by ID or fail.
     */
    public Uni<AccountResponse> findById(String accountId) {
        return Uni.createFrom().optional(viewRepository.findById(accountId))
                .onItem().ifNull().failWith(() -> new AccountNotFoundException(accountId))
                .map(AccountResponse::from);
    }

    /**
     * Find an account by user ID or fail.
     */
    public Uni<AccountResponse> findByUserId(String userId) {
        return Uni.createFrom().optional(viewRepository.findByUserId(userId))
                .onItem().ifNull().failWith(() -> new AccountNotFoundException("user:" + userId))
                .map(AccountResponse::from);
    }
}
