package com.wallet.account.domain;

import java.util.List;
import java.util.Optional;

/**
 * Port: Account read model (CQRS projection).
 * No framework dependency — implementation lives in infrastructure.
 */
public interface AccountViewRepository {

    /**
     * Find an account view by ID.
     */
    Optional<AccountView> findById(String accountId);

    /**
     * Find an account view by user ID.
     */
    Optional<AccountView> findByUserId(String userId);

    /**
     * Find all accounts with pagination.
     */
    List<AccountView> findAll(int offset, int limit);

    /**
     * Count all accounts.
     */
    long countAll();

    /**
     * Find accounts by user ID with pagination.
     */
    List<AccountView> findByUserIdPaginated(String userId, int offset, int limit);

    /**
     * Count accounts by user ID.
     */
    long countByUserId(String userId);

    /**
     * Save or update an account view.
     */
    AccountView save(AccountView view);
}
