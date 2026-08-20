package com.wallet.account.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventStoreJpaRepository extends JpaRepository<EventStoreEntity, String> {

    List<EventStoreEntity> findByAggregateIdOrderByVersionAsc(String aggregateId);
}
