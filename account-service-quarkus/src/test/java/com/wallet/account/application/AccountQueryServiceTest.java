package com.wallet.account.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wallet.account.domain.AccountView;
import com.wallet.account.domain.AccountViewRepository;
import com.wallet.account.domain.exception.AccountNotFoundException;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountQueryService")
class AccountQueryServiceTest {

    @Mock AccountViewRepository viewRepository;
    AccountQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new AccountQueryService(viewRepository);
    }

    @Test
    @DisplayName("should return account when found")
    void shouldReturnAccount() {
        AccountView view = AccountView.of("acc-001", "user-001",
                new BigDecimal("100.00"), "USD", "OPEN", 1, Instant.now());
        when(viewRepository.findById("acc-001")).thenReturn(Optional.of(view));

        UniAssertSubscriber<AccountResponse> subscriber = queryService.findById("acc-001")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        AccountResponse response = subscriber.assertCompleted().getItem();
        assertEquals("acc-001", response.accountId());
        assertEquals("user-001", response.userId());
    }

    @Test
    @DisplayName("should throw AccountNotFoundException when not found")
    void shouldThrowWhenNotFound() {
        when(viewRepository.findById("acc-999")).thenReturn(Optional.empty());

        UniAssertSubscriber<AccountResponse> subscriber = queryService.findById("acc-999")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(AccountNotFoundException.class);
    }
}
