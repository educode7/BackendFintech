package com.wallet.payment.infrastructure.observability;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import io.quarkus.runtime.StartupEvent;

/**
 * Business metrics for Payment Service.
 * <p>
 * Exposed via Micrometer Prometheus endpoint at /q/metrics.
 * Metrics:
 * - payment.processed.total (counter by status: SUCCESS, FAILED)
 * - payment.processing.duration (timer)
 * - payment.amount.histogram (distribution summary by currency)
 */
@ApplicationScoped
public class PaymentMetrics {

    private final MeterRegistry registry;

    private Counter paymentSuccessCounter;
    private Counter paymentFailedCounter;
    private Counter paymentDuplicateCounter;
    private Timer paymentProcessingTimer;

    @Inject
    public PaymentMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    void onStart(@Observes StartupEvent event) {
        paymentSuccessCounter = Counter.builder("payment.processed.total")
                .description("Total payments processed")
                .tag("status", "SUCCESS")
                .register(registry);

        paymentFailedCounter = Counter.builder("payment.processed.total")
                .description("Total payments processed")
                .tag("status", "FAILED")
                .register(registry);

        paymentDuplicateCounter = Counter.builder("payment.duplicate.total")
                .description("Duplicate payment attempts rejected")
                .register(registry);

        paymentProcessingTimer = Timer.builder("payment.processing.duration")
                .description("Payment processing latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void recordSuccess() {
        paymentSuccessCounter.increment();
    }

    public void recordFailure() {
        paymentFailedCounter.increment();
    }

    public void recordDuplicate() {
        paymentDuplicateCounter.increment();
    }

    public Timer.Sample startProcessing() {
        return Timer.start(registry);
    }

    public void stopProcessing(Timer.Sample sample) {
        sample.stop(paymentProcessingTimer);
    }
}
