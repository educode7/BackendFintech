package com.wallet.payment.infrastructure.web;

import com.wallet.payment.application.PaymentCommandService;
import com.wallet.payment.application.PaymentQueryService;
import com.wallet.payment.application.PaymentResponse;
import com.wallet.payment.application.ProcessPaymentCommand;
import com.wallet.payment.infrastructure.web.dto.ProcessPaymentRequest;
import com.wallet.payment.infrastructure.web.exception.MissingIdempotencyKeyException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * REST surface for the payment aggregate.
 *
 * Headers enforced:
 *  - Idempotency-Key: required on POST (we reject 400 if missing).
 *  - X-Correlation-Id: populated by CorrelationIdFilter and bound to a
 *    {@code ScopedValue} for the request's lifetime — visible via
 *    {@code CorrelationContext.currentOrNull()}.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentCommandService commandService;
    private final PaymentQueryService queryService;

    @PostMapping
    public ResponseEntity<PaymentResponse> create(
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @Valid @RequestBody ProcessPaymentRequest body
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException();
        }
        ProcessPaymentCommand cmd = new ProcessPaymentCommand(body.userId(), body.amount(), idempotencyKey);
        PaymentResponse response = commandService.process(cmd);
        return ResponseEntity
            .created(URI.create("/api/v1/payments/" + response.id()))
            .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> get(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.OK).body(queryService.findById(id));
    }
}
