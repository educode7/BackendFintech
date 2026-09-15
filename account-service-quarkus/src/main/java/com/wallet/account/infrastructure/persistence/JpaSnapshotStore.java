package com.wallet.account.infrastructure.persistence;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import com.wallet.account.domain.AccountSnapshot;
import com.wallet.account.domain.SnapshotStore;

/**
 * Infrastructure adapter: SnapshotStore implementation using JPA.
 * <p>
 * One row per account — latest snapshot only.
 * On save, merges (upsert) by primary key.
 */
@ApplicationScoped
public class JpaSnapshotStore implements SnapshotStore {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public Optional<AccountSnapshot> findLatest(String aggregateId) {
        AccountSnapshotEntity entity = entityManager.find(AccountSnapshotEntity.class, aggregateId);
        return Optional.ofNullable(entity).map(AccountSnapshotEntity::toDomain);
    }

    @Override
    @Transactional
    public void save(AccountSnapshot snapshot) {
        AccountSnapshotEntity entity = AccountSnapshotEntity.fromDomain(snapshot);
        entityManager.merge(entity);
        entityManager.flush();
    }
}
