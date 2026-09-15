package com.wallet.account.application;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.wallet.account.domain.AccountView;
import com.wallet.account.domain.AccountViewRepository;
import com.wallet.account.domain.exception.AccountNotFoundException;
import com.wallet.shared.api.PageResponse;

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

    /**
     * List all accounts with pagination.
     */
    public Uni<PageResponse<AccountResponse>> findAll(int page, int size) {
        return Uni.createFrom().item(() -> {
            int offset = page * size;
            List<AccountView> accounts = viewRepository.findAll(offset, size);
            long total = viewRepository.countAll();

            List<AccountResponse> items = accounts.stream()
                    .map(AccountResponse::from)
                    .toList();

            return PageResponse.of(items, total, page, size);
        });
    }
}
