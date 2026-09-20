package com.wallet.payment.application;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.wallet.payment.domain.Payment;
import com.wallet.payment.domain.PaymentRepository;
import com.wallet.payment.domain.exception.PaymentNotFoundException;
import com.wallet.shared.api.PageResponse;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

/**
 * Use case: retrieve payments.
 */
@Singleton
public class GetPaymentUseCase {

    private final PaymentRepository paymentRepository;

    @Inject
    public GetPaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /**
     * Find a payment by ID or fail with PaymentNotFoundException.
     */
    public Uni<PaymentResponse> execute(String paymentId) {
        return Uni.createFrom().item(() -> paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId)))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .map(PaymentResponse::from);
    }

    /**
     * List all payments with pagination.
     */
    public Uni<PageResponse<PaymentResponse>> findAll(int page, int size) {
        return Uni.createFrom().item(() -> {
            int offset = page * size;
            List<Payment> payments = paymentRepository.findAll(offset, size);
            long total = paymentRepository.countAll();

            List<PaymentResponse> items = payments.stream()
                    .map(PaymentResponse::from)
                    .toList();

            return PageResponse.of(items, total, page, size);
        }).runSubscriptionOn(Infrastructure.getDefaultExecutor());
    }
}
