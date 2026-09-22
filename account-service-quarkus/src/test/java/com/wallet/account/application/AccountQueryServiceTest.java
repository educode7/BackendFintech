package com.wallet.account.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
import com.wallet.shared.api.PageResponse;

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
                new BigDecimal("100.00"), "USD", "OPEN", 1, Instant.now(),
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null);
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

    @Test
    @DisplayName("should return paginated accounts")
    void shouldReturnPaginatedAccounts() {
        AccountView view1 = AccountView.of("acc-001", "user-001",
                new BigDecimal("100.00"), "USD", "OPEN", 1, Instant.now(),
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null);
        AccountView view2 = AccountView.of("acc-002", "user-002",
                new BigDecimal("200.00"), "USD", "OPEN", 1, Instant.now(),
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null);

        when(viewRepository.findAll(0, 10)).thenReturn(List.of(view1, view2));
        when(viewRepository.countAll()).thenReturn(2L);

        UniAssertSubscriber<PageResponse<AccountResponse>> subscriber = queryService.findAll(0, 10, null)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        PageResponse<AccountResponse> response = subscriber.assertCompleted().getItem();
        assertEquals(2, response.items().size());
        assertEquals(2L, response.total());
        assertEquals(0, response.page());
        assertEquals(10, response.size());
    }

    @Test
    @DisplayName("should return empty page when no accounts exist")
    void shouldReturnEmptyPage() {
        when(viewRepository.findAll(0, 10)).thenReturn(List.of());
        when(viewRepository.countAll()).thenReturn(0L);

        UniAssertSubscriber<PageResponse<AccountResponse>> subscriber = queryService.findAll(0, 10, null)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        PageResponse<AccountResponse> response = subscriber.assertCompleted().getItem();
        assertTrue(response.items().isEmpty());
        assertEquals(0L, response.total());
    }
}
