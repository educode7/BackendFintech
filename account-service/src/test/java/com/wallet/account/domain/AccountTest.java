package com.wallet.account.domain;

import com.wallet.account.domain.exception.InsufficientFundsException;
import com.wallet.shared.event.AccountOpenedEvent;
import com.wallet.shared.event.EventMetadata;
import com.wallet.shared.event.MoneyDepositedEvent;
import com.wallet.shared.event.MoneyWithdrawnEvent;
import com.wallet.shared.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private static Money money(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    private static EventMetadata metadata(int version) {
        return new EventMetadata("evt-" + version, Instant.now(), "corr-" + version, version);
    }

    private static AccountOpenedEvent opened(String accountId, String userId, Money balance) {
        return new AccountOpenedEvent(accountId, userId, balance, metadata(1));
    }

    private static MoneyDepositedEvent deposited(String accountId, Money amount, Money newBalance) {
        return new MoneyDepositedEvent(accountId, amount, newBalance, metadata(2));
    }

    private static MoneyWithdrawnEvent withdrawn(String accountId, Money amount, Money newBalance) {
        return new MoneyWithdrawnEvent(accountId, amount, newBalance, metadata(3));
    }

    @Nested
    @DisplayName("Estado inicial")
    class EstadoInicial {

        @Test
        @DisplayName("Account vacío tiene accountId/userId/balance null, status null, version 0")
        void empty_inicial() {
            Account account = new Account();

            assertThat(account.accountId()).isNull();
            assertThat(account.userId()).isNull();
            assertThat(account.balance()).isNull();
            assertThat(account.status()).isNull();
            assertThat(account.version()).isZero();
        }
    }

    @Nested
    @DisplayName("apply(AccountOpenedEvent)")
    class ApplyOpened {

        @Test
        @DisplayName("Inicializa la cuenta con id, user, balance y status OPEN; version=1")
        void opened_inicializa() {
            Account account = new Account();
            AccountOpenedEvent event = opened("acc-1", "user-1", money("100.00", "USD"));

            account.apply(event);

            assertThat(account.accountId()).isEqualTo("acc-1");
            assertThat(account.userId()).isEqualTo("user-1");
            assertThat(account.balance()).isEqualTo(money("100.00", "USD"));
            assertThat(account.status()).isEqualTo(Account.Status.OPEN);
            assertThat(account.version()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Documentado: aplicar AccountOpened dos veces sobreescribe sin lanzar excepción")
        void opened_dobleAplicacionSobreescribe() {
            Account account = new Account();
            account.apply(opened("acc-1", "user-1", money("100.00", "USD")));
            account.apply(opened("acc-1", "user-2", money("500.00", "EUR")));

            assertThat(account.userId()).isEqualTo("user-2");
            assertThat(account.balance()).isEqualTo(money("500.00", "EUR"));
            assertThat(account.version()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("apply(MoneyDepositedEvent)")
    class ApplyDeposited {

        @Test
        @DisplayName("Actualiza balance al newBalance del evento")
        void deposited_actualizaBalance() {
            Account account = new Account();
            account.apply(opened("acc-1", "user-1", money("100.00", "USD")));

            account.apply(deposited("acc-1", money("50.00", "USD"), money("150.00", "USD")));

            assertThat(account.balance()).isEqualTo(money("150.00", "USD"));
            assertThat(account.version()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Lanza excepción si se aplica antes de AccountOpened")
        void deposited_sinOpened_lanzaExcepcion() {
            Account account = new Account();

            assertThatThrownBy(() -> account.apply(deposited("acc-1", money("10.00", "USD"), money("10.00", "USD"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AccountOpened");
        }

        @Test
        @DisplayName("Lanza excepción si el accountId del evento no coincide")
        void deposited_accountIdDistinto_lanzaExcepcion() {
            Account account = new Account();
            account.apply(opened("acc-1", "user-1", money("100.00", "USD")));

            assertThatThrownBy(() -> account.apply(deposited("acc-2", money("10.00", "USD"), money("110.00", "USD"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match");
        }
    }

    @Nested
    @DisplayName("apply(MoneyWithdrawnEvent)")
    class ApplyWithdrawn {

        @Test
        @DisplayName("Actualiza balance al newBalance del evento")
        void withdrawn_actualizaBalance() {
            Account account = new Account();
            account.apply(opened("acc-1", "user-1", money("100.00", "USD")));
            account.apply(withdrawn("acc-1", money("40.00", "USD"), money("60.00", "USD")));

            assertThat(account.balance()).isEqualTo(money("60.00", "USD"));
            assertThat(account.version()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("assertCanWithdraw")
    class AssertCanWithdraw {

        @Test
        @DisplayName("Saldo suficiente: no lanza excepción")
        void saldoSuficiente_ok() {
            Account account = new Account();
            account.apply(opened("acc-1", "user-1", money("100.00", "USD")));

            account.assertCanWithdraw(money("50.00", "USD"));
        }

        @Test
        @DisplayName("Saldo insuficiente: lanza InsufficientFundsException")
        void saldoInsuficiente_lanzaExcepcion() {
            Account account = new Account();
            account.apply(opened("acc-1", "user-1", money("100.00", "USD")));

            assertThatThrownBy(() -> account.assertCanWithdraw(money("200.00", "USD")))
                .isInstanceOf(InsufficientFundsException.class);
        }

        @Test
        @DisplayName("Currency mismatch: lanza IllegalArgumentException")
        void currencyMismatch_lanzaExcepcion() {
            Account account = new Account();
            account.apply(opened("acc-1", "user-1", money("100.00", "USD")));

            assertThatThrownBy(() -> account.assertCanWithdraw(money("50.00", "EUR")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");
        }

        @Test
        @DisplayName("Cuenta sin OPEN: lanza IllegalStateException")
        void cuentaNoAbierta_lanzaExcepcion() {
            Account account = new Account();

            assertThatThrownBy(() -> account.assertCanWithdraw(money("10.00", "USD")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OPEN");
        }
    }

    @Nested
    @DisplayName("nextBalanceAfterDeposit / Withdrawal")
    class NextBalance {

        @Test
        @DisplayName("nextBalanceAfterDeposit retorna nuevo Money con suma")
        void nextBalance_deposit() {
            Account account = new Account();
            account.apply(opened("acc-1", "user-1", money("100.00", "USD")));

            Money next = account.nextBalanceAfterDeposit(money("50.00", "USD"));

            assertThat(next).isEqualTo(money("150.00", "USD"));
            assertThat(account.balance()).isEqualTo(money("100.00", "USD"));
        }

        @Test
        @DisplayName("nextBalanceAfterWithdrawal retorna nuevo Money con resta")
        void nextBalance_withdrawal() {
            Account account = new Account();
            account.apply(opened("acc-1", "user-1", money("100.00", "USD")));

            Money next = account.nextBalanceAfterWithdrawal(money("30.00", "USD"));

            assertThat(next).isEqualTo(money("70.00", "USD"));
            assertThat(account.balance()).isEqualTo(money("100.00", "USD"));
        }

        @Test
        @DisplayName("nextBalance con currency distinta lanza excepción")
        void nextBalance_currencyMismatch_lanzaExcepcion() {
            Account account = new Account();
            account.apply(opened("acc-1", "user-1", money("100.00", "USD")));

            assertThatThrownBy(() -> account.nextBalanceAfterDeposit(money("10.00", "EUR")))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> account.nextBalanceAfterWithdrawal(money("10.00", "EUR")))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Replay completo")
    class Replay {

        @Test
        @DisplayName("Replay [opened, deposited, withdrawn] deja el balance correcto")
        void replay_secuenciaCompleta() {
            Account account = new Account();
            account.apply(opened("acc-1", "user-1", money("100.00", "USD")));
            account.apply(deposited("acc-1", money("50.00", "USD"), money("150.00", "USD")));
            account.apply(withdrawn("acc-1", money("30.00", "USD"), money("120.00", "USD")));

            assertThat(account.accountId()).isEqualTo("acc-1");
            assertThat(account.userId()).isEqualTo("user-1");
            assertThat(account.balance()).isEqualTo(money("120.00", "USD"));
            assertThat(account.status()).isEqualTo(Account.Status.OPEN);
            assertThat(account.version()).isEqualTo(3L);
        }

        @Test
        @DisplayName("Replay con eventos desordenados (deposited antes de opened) lanza IllegalStateException")
        void replay_ordenIncorrecto_lanzaExcepcion() {
            Account account = new Account();

            assertThatThrownBy(() -> account.apply(deposited("acc-1", money("50.00", "USD"), money("50.00", "USD"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AccountOpened");
        }
    }
}
