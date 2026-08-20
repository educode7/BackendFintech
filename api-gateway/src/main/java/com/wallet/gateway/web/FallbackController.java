package com.wallet.gateway.web;

import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Fallback endpoints used when a circuit breaker is OPEN.
 * Spring Cloud Gateway forwards to /fallback/{service} and we return
 * an RFC 9457 ProblemDetail so the client always sees the same shape.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping(value = "/payment", produces = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
    public ResponseEntity<ProblemDetail> payment() {
        return build("paymentService", "Payment service unavailable, please retry shortly.");
    }

    @GetMapping(value = "/account", produces = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
    public ResponseEntity<ProblemDetail> account() {
        return build("accountService", "Account service unavailable, please retry shortly.");
    }

    @GetMapping(value = "/notification", produces = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
    public ResponseEntity<ProblemDetail> notification() {
        return build("notificationService", "Notification service unavailable, please retry shortly.");
    }

    private ResponseEntity<ProblemDetail> build(String instance, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, detail);
        pd.setType(URI.create("about:blank"));
        pd.setTitle("Service Unavailable");
        pd.setInstance(URI.create(instance));
        pd.setProperty("code", "CIRCUIT_OPEN");
        return ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE).body(pd);
    }
}
