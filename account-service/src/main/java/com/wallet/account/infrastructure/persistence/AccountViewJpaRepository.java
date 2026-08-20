package com.wallet.account.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountViewJpaRepository extends JpaRepository<AccountViewEntity, String> {
}
