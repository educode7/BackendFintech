package com.wallet.account.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wallet.account.domain.AccountView;
import com.wallet.account.domain.AccountViewRepository;
import com.wallet.account.domain.EventStore;
import com.wallet.account.domain.SnapshotStore;
import com.wallet.shared.event.AccountEvent;
import com.wallet.shared.event.AccountOpenedEvent;
import com.wallet.shared.event.EventMetadata;
import com.wallet.shared.money.Money;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountCommandService")
class AccountCommandServiceTest {

    @Mock EventStore eventStore;
    @Mock AccountViewRepository viewRepository;
    @Mock SnapshotStore snapshotStore;

    AccountCommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new AccountCommandService(eventStore, viewRepository, snapshotStore);
    }

    @Test
    @DisplayName("should open account and persist projection")
    void shouldOpenAccount() {
        when(viewRepository.save(any(AccountView.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AccountCommand.OpenAccount command = new AccountCommand.OpenAccount(
                "user-001", new Money(new BigDecimal("100.00"), "USD"), "req-001");

        UniAssertSubscriber<AccountResponse> subscriber = commandService.openAccount(command)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        AccountResponse response = subscriber.assertCompleted().getItem();
        assertNotNull(response);
        assertEquals("user-001", response.userId());
        assertEquals(new BigDecimal("100.00"), response.balanceAmount());
        assertEquals("USD", response.balanceCurrency());
        assertEquals("OPEN", response.status());

        verify(eventStore).appendEvents(anyString(), anyList(), eq(0L));
        verify(viewRepository).save(any(AccountView.class));
    }

    @Test
    @DisplayName("should deposit into existing account")
    void shouldDeposit() {
        // Setup: account exists in event store
        AccountOpenedEvent opened = new AccountOpenedEvent("acc-001", "user-001",
                new Money(new BigDecimal("100.00"), "USD"),
                EventMetadata.create(null, 1));
        when(eventStore.loadEvents("acc-001")).thenReturn(List.of(opened));
        when(viewRepository.save(any(AccountView.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AccountCommand.Deposit command = new AccountCommand.Deposit(
                "acc-001", new BigDecimal("50.00"), "USD", "req-002");

        UniAssertSubscriber<AccountResponse> subscriber = commandService.deposit(command)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        AccountResponse response = subscriber.assertCompleted().getItem();
        assertEquals(new BigDecimal("150.00"), response.balanceAmount());

        verify(eventStore).appendEvents(eq("acc-001"), anyList(), anyLong());
        verify(viewRepository).save(any(AccountView.class));
    }

    @Test
    @DisplayName("should fail deposit on non-existent account")
    void shouldFailDepositOnMissingAccount() {
        when(eventStore.loadEvents("acc-999")).thenReturn(List.of());

        AccountCommand.Deposit command = new AccountCommand.Deposit(
                "acc-999", new BigDecimal("50.00"), "USD", "req-003");

        UniAssertSubscriber<AccountResponse> subscriber = commandService.deposit(command)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(com.wallet.account.domain.exception.AccountNotFoundException.class);
    }
}
