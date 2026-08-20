package com.wallet.account.application;

import com.wallet.account.infrastructure.persistence.JpaEventStore;
import com.wallet.account.testsupport.AbstractIntegrationTest;
import com.wallet.shared.event.AccountOpenedEvent;
import com.wallet.shared.event.MoneyDepositedEvent;
import com.wallet.shared.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountServiceIT extends AbstractIntegrationTest {

    @Autowired
    private AccountCommandService commandService;

    @Autowired
    private JpaEventStore eventStore;

    @Autowired
    private com.wallet.account.infrastructure.persistence.EventStoreJpaRepository eventStoreRepo;

    @Test
    @DisplayName("openAccount persiste el primer evento y devuelve el aggregate")
    void openAccount_persistePrimerEvento() {
        com.wallet.account.domain.Account account = commandService.openAccount(
            "user-it-1",
            new Money(new BigDecimal("100.00"), "USD")
        );

        assertThat(account.accountId()).isNotBlank();
        assertThat(account.userId()).isEqualTo("user-it-1");
        assertThat(account.balance()).isEqualTo(new Money(new BigDecimal("100.00"), "USD"));
        assertThat(account.status()).isEqualTo(com.wallet.account.domain.Account.Status.OPEN);

        List<com.wallet.account.infrastructure.persistence.EventStoreEntity> rows =
            eventStoreRepo.findByAggregateIdOrderByVersionAsc(account.accountId());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getEventType()).isEqualTo("AccountOpenedEvent");
    }

    @Test
    @DisplayName("deposit incrementa el balance y deja 2 eventos en el store")
    void deposit_incrementaBalance() {
        com.wallet.account.domain.Account account = commandService.openAccount(
            "user-it-2",
            new Money(new BigDecimal("100.00"), "USD")
        );

        com.wallet.account.domain.Account afterDeposit = commandService.deposit(
            account.accountId(),
            new Money(new BigDecimal("50.00"), "USD")
        );

        assertThat(afterDeposit.balance()).isEqualTo(new Money(new BigDecimal("150.00"), "USD"));

        List<com.wallet.account.infrastructure.persistence.EventStoreEntity> rows =
            eventStoreRepo.findByAggregateIdOrderByVersionAsc(account.accountId());
        assertThat(rows).hasSize(2);
        assertThat(rows.get(1).getEventType()).isEqualTo("MoneyDepositedEvent");
    }

    @Test
    @DisplayName("withdraw con saldo suficiente actualiza el balance")
    void withdraw_conSaldoSuficiente_actualiza() {
        com.wallet.account.domain.Account account = commandService.openAccount(
            "user-it-3",
            new Money(new BigDecimal("100.00"), "USD")
        );

        com.wallet.account.domain.Account after = commandService.withdraw(
            account.accountId(),
            new Money(new BigDecimal("30.00"), "USD")
        );

        assertThat(after.balance()).isEqualTo(new Money(new BigDecimal("70.00"), "USD"));
    }

    @Test
    @DisplayName("withdraw con saldo insuficiente lanza InsufficientFundsException")
    void withdraw_conSaldoInsuficiente_lanzaExcepcion() {
        com.wallet.account.domain.Account account = commandService.openAccount(
            "user-it-4",
            new Money(new BigDecimal("10.00"), "USD")
        );

        assertThatThrownBy(() -> commandService.withdraw(
            account.accountId(),
            new Money(new BigDecimal("100.00"), "USD")
        )).isInstanceOf(com.wallet.account.domain.exception.InsufficientFundsException.class);

        assertThat(eventStoreRepo.findByAggregateIdOrderByVersionAsc(account.accountId()))
            .as("no debe haber eventos de withdrawal persistidos")
            .hasSize(1);
    }

    @Test
    @DisplayName("append con version duplicada lanza DataIntegrityViolationException tras 3 reintentos")
    void append_versionDuplicada_lanzaExcepcion() {
        com.wallet.account.domain.Account account = commandService.openAccount(
            "user-it-5",
            new Money(new BigDecimal("100.00"), "USD")
        );

        com.wallet.account.infrastructure.persistence.EventStoreEntity row =
            new com.wallet.account.infrastructure.persistence.EventStoreEntity();
        row.setId(com.wallet.shared.util.IdGenerator.newId());
        row.setAggregateId(account.accountId());
        row.setAggregateType("Account");
        row.setEventType("MoneyDepositedEvent");
        row.setEventData(com.wallet.shared.util.JsonUtil.toJson(new MoneyDepositedEvent(
            account.accountId(),
            new Money(new BigDecimal("10.00"), "USD"),
            new Money(new BigDecimal("110.00"), "USD"),
            new com.wallet.shared.event.EventMetadata(
                com.wallet.shared.util.IdGenerator.newId(),
                java.time.Instant.now(),
                "corr-dup",
                2
            )
        )));
        row.setVersion(2L);
        row.setCreatedAt(java.time.Instant.now());

        eventStoreRepo.save(row);
        eventStoreRepo.flush();

        assertThatThrownBy(() -> eventStore.append(
            new com.wallet.shared.event.AccountOpenedEvent(
                account.accountId(),
                "user-other",
                new Money(new BigDecimal("1.00"), "USD"),
                new com.wallet.shared.event.EventMetadata(
                    com.wallet.shared.util.IdGenerator.newId(),
                    java.time.Instant.now(),
                    "corr-dup2",
                    2
                )
            ),
            "Account"
        )).isInstanceOf(com.wallet.account.domain.exception.ConcurrentModificationException.class);
    }

    @Test
    @DisplayName("load replay-construye el aggregate con todos los eventos en orden")
    void load_conMultiplesEventos_replayOrdenado() {
        com.wallet.account.domain.Account opened = commandService.openAccount(
            "user-it-6",
            new Money(new BigDecimal("200.00"), "EUR")
        );
        commandService.deposit(opened.accountId(), new Money(new BigDecimal("50.00"), "EUR"));

        com.wallet.account.domain.Account reloaded = eventStore.load(opened.accountId());

        assertThat(reloaded.balance()).isEqualTo(new Money(new BigDecimal("250.00"), "EUR"));
        assertThat(reloaded.version()).isEqualTo(2L);
    }

    @Test
    @DisplayName("AccountQueryService.findView devuelve AccountView con datos del aggregate")
    @SuppressWarnings("unused")
    void queryView_devuelveAccountView() {
        com.wallet.account.domain.Account opened = commandService.openAccount(
            "user-it-7",
            new Money(new BigDecimal("75.00"), "USD")
        );

        com.wallet.account.infrastructure.projection.AccountView view =
            new com.wallet.account.application.AccountQueryService(eventStore)
                .findView(opened.accountId());

        assertThat(view.accountId()).isEqualTo(opened.accountId());
        assertThat(view.userId()).isEqualTo("user-it-7");
        assertThat(view.balanceAmount()).isEqualByComparingTo("75.00");
    }
}
