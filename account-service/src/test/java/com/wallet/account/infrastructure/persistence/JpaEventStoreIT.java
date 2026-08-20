package com.wallet.account.infrastructure.persistence;

import com.wallet.account.domain.Account;
import com.wallet.account.domain.exception.AccountNotFoundException;
import com.wallet.account.testsupport.AbstractIntegrationTest;
import com.wallet.shared.event.AccountOpenedEvent;
import com.wallet.shared.event.EventMetadata;
import com.wallet.shared.event.MoneyDepositedEvent;
import com.wallet.shared.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JpaEventStoreIT extends AbstractIntegrationTest {

    @Autowired
    private JpaEventStore store;

    @Autowired
    private EventStoreJpaRepository repo;

    @Test
    @DisplayName("append + load replay-construye el aggregate")
    void append_yLoad_replayCorrecto() {
        AccountOpenedEvent opened = newOpened("acc-it-1", "user-1", "100.00");
        store.append(opened, "Account");

        Account account = store.load("acc-it-1");

        assertThat(account.accountId()).isEqualTo("acc-it-1");
        assertThat(account.userId()).isEqualTo("user-1");
        assertThat(account.balance()).isEqualTo(new Money(new BigDecimal("100.00"), "USD"));
    }

    @Test
    @DisplayName("load con múltiples eventos replay construye el balance correcto")
    void load_conMultiplesEventos_replay() {
        store.append(newOpened("acc-it-2", "user-1", "100.00"), "Account");
        store.append(newDeposited("acc-it-2", "100.00", "200.00"), "Account");
        store.append(newDeposited("acc-it-2", "50.00", "250.00"), "Account");

        Account account = store.load("acc-it-2");

        assertThat(account.balance()).isEqualTo(new Money(new BigDecimal("250.00"), "USD"));
        assertThat(account.version()).isEqualTo(3L);
    }

    @Test
    @DisplayName("load sin eventos lanza AccountNotFoundException")
    void load_sinEventos_lanzaExcepcion() {
        assertThatThrownBy(() -> store.load("does-not-exist"))
            .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    @DisplayName("exists retorna false cuando no hay eventos")
    void exists_false() {
        assertThat(store.exists("does-not-exist")).isFalse();
    }

    @Test
    @DisplayName("exists retorna true tras append")
    void exists_true() {
        store.append(newOpened("acc-it-3", "user-1", "10.00"), "Account");
        assertThat(store.exists("acc-it-3")).isTrue();
    }

    @Test
    @DisplayName("Eventos persistidos tienen la tupla (aggregate_id, version) UNIQUE")
    void versionDuplicada_lanzaExcepcion() {
        store.append(newOpened("acc-it-4", "user-1", "10.00"), "Account");
        store.append(newDeposited("acc-it-4", "10.00", "20.00"), "Account");

        // Insertamos manualmente una fila con version=1 duplicada
        EventStoreEntity row = new EventStoreEntity();
        row.setId(com.wallet.shared.util.IdGenerator.newId());
        row.setAggregateId("acc-it-4");
        row.setAggregateType("Account");
        row.setEventType("MoneyDepositedEvent");
        row.setEventData("{}");
        row.setVersion(1L);
        row.setCreatedAt(Instant.now());

        org.assertj.core.api.Assertions
            .assertThatThrownBy(() -> { repo.saveAndFlush(row); })
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        List<EventStoreEntity> rows = repo.findByAggregateIdOrderByVersionAsc("acc-it-4");
        assertThat(rows).hasSize(2);
    }

    private static AccountOpenedEvent newOpened(String accountId, String userId, String amount) {
        return new AccountOpenedEvent(
            accountId, userId,
            new Money(new BigDecimal(amount), "USD"),
            new EventMetadata(com.wallet.shared.util.IdGenerator.newId(), Instant.now(), "corr-1", 1)
        );
    }

    private static MoneyDepositedEvent newDeposited(String accountId, String amount, String newBalance) {
        return new MoneyDepositedEvent(
            accountId,
            new Money(new BigDecimal(amount), "USD"),
            new Money(new BigDecimal(newBalance), "USD"),
            new EventMetadata(com.wallet.shared.util.IdGenerator.newId(), Instant.now(), "corr-1", 2)
        );
    }
}
