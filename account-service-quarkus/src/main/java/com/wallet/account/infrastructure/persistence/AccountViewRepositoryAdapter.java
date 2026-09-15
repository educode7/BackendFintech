package com.wallet.account.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import com.wallet.account.domain.AccountView;
import com.wallet.account.domain.AccountViewRepository;

/**
 * Infrastructure adapter: AccountViewRepository using Hibernate Reactive.
 */
@ApplicationScoped
public class AccountViewRepositoryAdapter implements AccountViewRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public Optional<AccountView> findById(String accountId) {
        try {
            AccountViewEntity entity = entityManager.find(AccountViewEntity.class, accountId);
            return Optional.ofNullable(entity).map(AccountViewEntity::toDomain);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<AccountView> findByUserId(String userId) {
        try {
            AccountViewEntity entity = entityManager
                    .createQuery("SELECT a FROM AccountViewEntity a WHERE a.userId = :userId",
                            AccountViewEntity.class)
                    .setParameter("userId", userId)
                    .getSingleResult();
            return Optional.ofNullable(entity).map(AccountViewEntity::toDomain);
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<AccountView> findAll(int offset, int limit) {
        return entityManager
                .createQuery("SELECT a FROM AccountViewEntity a ORDER BY a.lastUpdated DESC",
                        AccountViewEntity.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList()
                .stream()
                .map(AccountViewEntity::toDomain)
                .toList();
    }

    @Override
    public long countAll() {
        return entityManager
                .createQuery("SELECT COUNT(a) FROM AccountViewEntity a", Long.class)
                .getSingleResult();
    }

    @Override
    @Transactional
    public AccountView save(AccountView view) {
        AccountViewEntity entity = AccountViewEntity.fromDomain(view);
        if (entityManager.contains(entity)) {
            entityManager.merge(entity);
        } else {
            entityManager.persist(entity);
        }
        entityManager.flush();
        return entity.toDomain();
    }
}
