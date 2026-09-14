package com.wallet.payment.application;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.wallet.payment.domain.Payment;
import com.wallet.payment.domain.PaymentRepository;
import com.wallet.payment.domain.exception.PaymentNotFoundException;

import io.smallrye.mutiny.Uni;

/**
 * Use case: retrieve a payment by ID.
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
                .map(PaymentResponse::from);
    }
}
