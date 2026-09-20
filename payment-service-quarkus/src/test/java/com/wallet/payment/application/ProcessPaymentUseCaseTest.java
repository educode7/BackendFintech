package com.wallet.payment.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

import com.wallet.payment.domain.IdempotencyStore;
import com.wallet.payment.domain.OutboxEvent;
import com.wallet.payment.domain.OutboxRepository;
import com.wallet.payment.domain.Payment;
import com.wallet.payment.domain.PaymentRepository;
import com.wallet.payment.domain.exception.DuplicatePaymentException;
import com.wallet.shared.money.Money;

import jakarta.transaction.UserTransaction;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessPaymentUseCase")
class ProcessPaymentUseCaseTest {

    @Mock PaymentRepository paymentRepository;
    @Mock IdempotencyStore idempotencyStore;
    @Mock OutboxRepository outboxRepository;
    @Mock UserTransaction userTransaction;

    ProcessPaymentUseCase useCase;

    @BeforeEach
    void setUp() throws Exception {
        useCase = new ProcessPaymentUseCase(paymentRepository, idempotencyStore, outboxRepository, userTransaction);
        useCase.idempotencyTtlHours = 24;
        lenient().when(userTransaction.getStatus()).thenReturn(0); // STATUS_NO_TRANSACTION
        lenient().doNothing().when(userTransaction).begin();
        lenient().doNothing().when(userTransaction).commit();
    }

    @Test
    @DisplayName("should process payment when idempotency slot is available")
    void shouldProcessPayment() {
        // Given
        Money amount = new Money(new BigDecimal("25.50"), "USD");
        ProcessPaymentCommand command = new ProcessPaymentCommand("acc-001", "user-001", amount, "key-001");

        when(idempotencyStore.claim("key-001", 24))
                .thenReturn(IdempotencyStore.SlotState.IN_PROGRESS);
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment p = invocation.getArgument(0);
                    return Payment.of(p.id(), p.accountId(), p.userId(), p.amount(), p.idempotencyKey(),
                            p.status(), p.version(), p.createdAt(), p.updatedAt());
                });

        // When
        UniAssertSubscriber<PaymentResponse> subscriber = useCase.execute(command, "corr-001")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        PaymentResponse response = subscriber.assertCompleted().getItem();
        assertNotNull(response);
        assertEquals("user-001", response.userId());
        assertEquals(new BigDecimal("25.50"), response.amount());
        assertEquals("USD", response.currency());
        assertEquals("COMPLETED", response.status());

        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(outboxRepository).save(any(OutboxEvent.class));
        verify(idempotencyStore).complete(eq("key-001"), anyString(), eq(24));
    }

    @Test
    @DisplayName("should replay cached response when slot is COMPLETED")
    void shouldReplayCachedResponse() {
        // Given
        Money amount = new Money(new BigDecimal("25.50"), "USD");
        ProcessPaymentCommand command = new ProcessPaymentCommand("acc-001", "user-001", amount, "key-001");

        String cachedResponse = "{\"id\":\"pay-001\",\"accountId\":\"acc-001\",\"userId\":\"user-001\",\"amount\":25.50,\"currency\":\"USD\",\"status\":\"COMPLETED\",\"createdAt\":\"2026-01-01T00:00:00Z\",\"updatedAt\":\"2026-01-01T00:00:00Z\"}";

        when(idempotencyStore.claim("key-001", 24))
                .thenReturn(IdempotencyStore.SlotState.COMPLETED);
        when(idempotencyStore.getCachedResponse("key-001"))
                .thenReturn(Optional.of(cachedResponse));

        // When
        UniAssertSubscriber<PaymentResponse> subscriber = useCase.execute(command, "corr-001")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        PaymentResponse response = subscriber.assertCompleted().getItem();
        assertNotNull(response);
        assertEquals("pay-001", response.id());
        assertEquals("COMPLETED", response.status());

        verify(paymentRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw DuplicatePaymentException when slot is DUPLICATE")
    void shouldThrowOnInProgress() {
        // Given
        Money amount = new Money(new BigDecimal("25.50"), "USD");
        ProcessPaymentCommand command = new ProcessPaymentCommand("acc-001", "user-001", amount, "key-001");

        when(idempotencyStore.claim("key-001", 24))
                .thenReturn(IdempotencyStore.SlotState.DUPLICATE);

        // When
        UniAssertSubscriber<PaymentResponse> subscriber = useCase.execute(command, "corr-001")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        subscriber.assertFailedWith(DuplicatePaymentException.class);

        verify(paymentRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }
}
