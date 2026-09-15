package com.wallet.payment.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

import com.wallet.payment.domain.Payment;
import com.wallet.payment.domain.PaymentRepository;
import com.wallet.payment.domain.exception.PaymentNotFoundException;
import com.wallet.shared.api.PageResponse;
import com.wallet.shared.money.Money;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetPaymentUseCase")
class GetPaymentUseCaseTest {

    @Mock PaymentRepository paymentRepository;
    GetPaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetPaymentUseCase(paymentRepository);
    }

    @Test
    @DisplayName("should return payment when found")
    void shouldReturnPayment() {
        Money amount = new Money(new BigDecimal("25.50"), "USD");
        Payment payment = Payment.create("pay-001", "acc-001", "user-001", amount, "key-001");

        when(paymentRepository.findById("pay-001")).thenReturn(Optional.of(payment));

        UniAssertSubscriber<PaymentResponse> subscriber = useCase.execute("pay-001")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        PaymentResponse response = subscriber.assertCompleted().getItem();
        assertEquals("pay-001", response.id());
        assertEquals("acc-001", response.accountId());
        assertEquals("user-001", response.userId());
        assertEquals(new BigDecimal("25.50"), response.amount());
    }

    @Test
    @DisplayName("should throw PaymentNotFoundException when not found")
    void shouldThrowWhenNotFound() {
        when(paymentRepository.findById("pay-999")).thenReturn(Optional.empty());

        UniAssertSubscriber<PaymentResponse> subscriber = useCase.execute("pay-999")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(PaymentNotFoundException.class);
    }

    @Test
    @DisplayName("should return paginated payments")
    void shouldReturnPaginatedPayments() {
        Money amount = new Money(new BigDecimal("25.50"), "USD");
        Payment payment1 = Payment.create("pay-001", "acc-001", "user-001", amount, "key-001");
        Payment payment2 = Payment.create("pay-002", "acc-002", "user-002", amount, "key-002");

        when(paymentRepository.findAll(0, 10)).thenReturn(List.of(payment1, payment2));
        when(paymentRepository.countAll()).thenReturn(2L);

        UniAssertSubscriber<PageResponse<PaymentResponse>> subscriber = useCase.findAll(0, 10)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        PageResponse<PaymentResponse> response = subscriber.assertCompleted().getItem();
        assertEquals(2, response.items().size());
        assertEquals(2L, response.total());
        assertEquals(0, response.page());
        assertEquals(10, response.size());
    }

    @Test
    @DisplayName("should return empty page when no payments exist")
    void shouldReturnEmptyPage() {
        when(paymentRepository.findAll(0, 10)).thenReturn(List.of());
        when(paymentRepository.countAll()).thenReturn(0L);

        UniAssertSubscriber<PageResponse<PaymentResponse>> subscriber = useCase.findAll(0, 10)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        PageResponse<PaymentResponse> response = subscriber.assertCompleted().getItem();
        assertTrue(response.items().isEmpty());
        assertEquals(0L, response.total());
    }
}
