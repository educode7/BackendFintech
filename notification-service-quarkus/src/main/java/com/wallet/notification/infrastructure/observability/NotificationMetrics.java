package com.wallet.notification.infrastructure.observability;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.runtime.StartupEvent;

/**
 * Business metrics for Notification Service.
 * <p>
 * Metrics:
 * - notification.processed.total (counter by status: SENT, FAILED)
 * - notification.inbox.duplicate.total (counter)
 * - notification.dispatch.duration (timer)
 */
@ApplicationScoped
public class NotificationMetrics {

    private final MeterRegistry registry;

    private Counter sentCounter;
    private Counter failedCounter;
    private Counter duplicateCounter;
    private Timer dispatchTimer;

    @Inject
    public NotificationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    void onStart(@Observes StartupEvent event) {
        sentCounter = Counter.builder("notification.processed.total")
                .tag("status", "SENT")
                .register(registry);

        failedCounter = Counter.builder("notification.processed.total")
                .tag("status", "FAILED")
                .register(registry);

        duplicateCounter = Counter.builder("notification.inbox.duplicate.total")
                .register(registry);

        dispatchTimer = Timer.builder("notification.dispatch.duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void recordSent() { sentCounter.increment(); }
    public void recordFailed() { failedCounter.increment(); }
    public void recordDuplicate() { duplicateCounter.increment(); }
    public Timer.Sample startDispatch() { return Timer.start(registry); }
    public void stopDispatch(Timer.Sample sample) { sample.stop(dispatchTimer); }
}
