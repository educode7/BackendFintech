package com.wallet.payment.application;

import com.wallet.payment.infrastructure.persistence.PaymentJpaRepository;
import com.wallet.payment.infrastructure.web.exception.PaymentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentQueryService {

    private final PaymentJpaRepository repository;

    @Transactional(readOnly = true)
    public PaymentResponse findById(String id) {
        return repository.findById(id)
            .map(PaymentResponse::from)
            .orElseThrow(() -> new PaymentNotFoundException(id));
    }
}
