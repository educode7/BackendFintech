package com.wallet.account.application;

import com.wallet.account.domain.Account;
import com.wallet.account.infrastructure.kafka.AccountEventPublisher;
import com.wallet.account.infrastructure.persistence.JpaEventStore;
import com.wallet.account.infrastructure.projection.AccountProjectionService;
import com.wallet.shared.event.AccountOpenedEvent;
import com.wallet.shared.event.MoneyDepositedEvent;
import com.wallet.shared.event.MoneyWithdrawnEvent;
import com.wallet.shared.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AccountCommandServiceTest {

    @Mock
    private JpaEventStore eventStore;

    @Mock
    private AccountEventPublisher publisher;

    @Mock
    private AccountProjectionService projection;

    @InjectMocks
    private AccountCommandService service;

    @Test
    @DisplayName("openAccount: persiste evento, publica en Kafka y proyecta")
    void openAccount_persiste_publica_proyecta() {
        Money initial = new Money(new BigDecimal("100.00"), "USD");
        given(eventStore.load(org.mockito.ArgumentMatchers.anyString()))
            .willAnswer(inv -> newReloadedAccount(inv.getArgument(0), "user-1", initial));

        Account result = service.openAccount("user-1", initial);

        ArgumentCaptor<AccountOpenedEvent> eventCaptor = ArgumentCaptor.forClass(AccountOpenedEvent.class);
        verify(eventStore).append(eventCaptor.capture(), org.mockito.ArgumentMatchers.eq("Account"));
        assertThat(eventCaptor.getValue().userId()).isEqualTo("user-1");
        assertThat(eventCaptor.getValue().initialBalance()).isEqualTo(initial);

        verify(publisher).publish(eventCaptor.getValue());
        verify(projection).project(eventCaptor.getValue());

        assertThat(result.accountId()).isEqualTo(eventCaptor.getValue().accountId());
    }

    @Test
    @DisplayName("deposit: carga el aggregate, calcula newBalance, persiste y proyecta")
    void deposit_calcula_persiste_proyecta() {
        Money initial = new Money(new BigDecimal("100.00"), "USD");
        Account reloaded = newReloadedAccount("acc-1", "user-1", initial);
        given(eventStore.load("acc-1")).willReturn(reloaded);

        Money amount = new Money(new BigDecimal("50.00"), "USD");
        service.deposit("acc-1", amount);

        ArgumentCaptor<MoneyDepositedEvent> eventCaptor = ArgumentCaptor.forClass(MoneyDepositedEvent.class);
        verify(eventStore).append(eventCaptor.capture(), org.mockito.ArgumentMatchers.eq("Account"));
        MoneyDepositedEvent captured = eventCaptor.getValue();
        assertThat(captured.amount()).isEqualTo(amount);
        assertThat(captured.newBalance().amount()).isEqualByComparingTo("150.00");

        verify(publisher).publish(captured);
        verify(projection).project(captured);
    }

    @Test
    @DisplayName("withdraw: assertCanWithdraw ok, persiste withdrawal y proyecta")
    void withdraw_ok_persiste_proyecta() {
        Money initial = new Money(new BigDecimal("100.00"), "USD");
        Account reloaded = newReloadedAccount("acc-1", "user-1", initial);
        given(eventStore.load("acc-1")).willReturn(reloaded);

        Money amount = new Money(new BigDecimal("30.00"), "USD");
        service.withdraw("acc-1", amount);

        ArgumentCaptor<MoneyWithdrawnEvent> eventCaptor = ArgumentCaptor.forClass(MoneyWithdrawnEvent.class);
        verify(eventStore).append(eventCaptor.capture(), org.mockito.ArgumentMatchers.eq("Account"));
        MoneyWithdrawnEvent captured = eventCaptor.getValue();
        assertThat(captured.amount()).isEqualTo(amount);
        assertThat(captured.newBalance().amount()).isEqualByComparingTo("70.00");

        verify(publisher).publish(captured);
        verify(projection).project(captured);
    }

    @Test
    @DisplayName("withdraw: saldo insuficiente lanza InsufficientFundsException y no persiste")
    void withdraw_insuficiente_noPersiste() {
        Money initial = new Money(new BigDecimal("50.00"), "USD");
        Account reloaded = newReloadedAccount("acc-1", "user-1", initial);
        given(eventStore.load("acc-1")).willReturn(reloaded);

        try {
            service.withdraw("acc-1", new Money(new BigDecimal("100.00"), "USD"));
        } catch (com.wallet.account.domain.exception.InsufficientFundsException expected) {
            // expected
        }

        verify(eventStore, org.mockito.Mockito.never()).append(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
        verifyNoInteractions(publisher);
        verifyNoInteractions(projection);
    }

    @Test
    @DisplayName("handlePaymentCompleted: solo loguea — operación vacía en producción")
    void handlePaymentCompleted_soloLoguea() {
        service.handlePaymentCompleted("user-1", new Money(new BigDecimal("10"), "USD"));

        verifyNoInteractions(eventStore);
        verifyNoInteractions(publisher);
        verifyNoInteractions(projection);
    }

    private static Account newReloadedAccount(String accountId, String userId, Money balance) {
        Account account = new Account();
        account.apply(new AccountOpenedEvent(
            accountId, userId, balance,
            new com.wallet.shared.event.EventMetadata("evt-1", java.time.Instant.now(), "corr-1", 1)
        ));
        return account;
    }
}
