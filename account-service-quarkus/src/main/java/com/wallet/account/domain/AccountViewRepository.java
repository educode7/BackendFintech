package com.wallet.account.domain;

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
     * Save or update an account view.
     */
    AccountView save(AccountView view);
}
