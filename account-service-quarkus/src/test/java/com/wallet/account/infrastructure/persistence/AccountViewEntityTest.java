package com.wallet.account.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wallet.account.domain.AccountView;

@DisplayName("AccountViewEntity mapping")
class AccountViewEntityTest {

    @Test
    @DisplayName("should map domain to entity")
    void domainToEntity() {
        AccountView view = new AccountView("acc-001", "user-001",
                new BigDecimal("100.00"), "USD", "OPEN", 1, Instant.now(),
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null);
        AccountViewEntity entity = AccountViewEntity.fromDomain(view);

        assertEquals("acc-001", entity.getAccountId());
        assertEquals("user-001", entity.getUserId());
        assertEquals(new BigDecimal("100.00"), entity.getBalanceAmount());
        assertEquals("USD", entity.getBalanceCurrency());
        assertEquals("OPEN", entity.getStatus());
        assertEquals(1, entity.getVersion());
    }

    @Test
    @DisplayName("should map entity to domain and back")
    void entityToDomain() {
        AccountView view = new AccountView("acc-001", "user-001",
                new BigDecimal("100.00"), "USD", "OPEN", 1, Instant.now(),
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null);
        AccountViewEntity entity = AccountViewEntity.fromDomain(view);

        AccountView roundTrip = entity.toDomain();

        assertEquals("acc-001", roundTrip.accountId());
        assertEquals("user-001", roundTrip.userId());
        assertEquals("OPEN", roundTrip.status());
        assertEquals(1, roundTrip.version());
    }
}
