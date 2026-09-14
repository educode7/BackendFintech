package com.wallet.gateway.observability;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.runtime.StartupEvent;

/**
 * Business metrics for API Gateway.
 * <p>
 * Metrics:
 * - gateway.request.total (counter by method, path, status)
 * - gateway.request.duration (timer)
 * - gateway.proxy.failure.total (counter by service)
 */
@ApplicationScoped
public class GatewayMetrics {

    private final MeterRegistry registry;

    private Counter proxyFailureCounter;

    @Inject
    public GatewayMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    void onStart(@Observes StartupEvent event) {
        proxyFailureCounter = Counter.builder("gateway.proxy.failure.total")
                .description("Total proxy failures to backend services")
                .register(registry);
    }

    public void recordProxyFailure(String service) {
        Counter.builder("gateway.proxy.failure.total")
                .tag("service", service)
                .register(registry)
                .increment();
    }
}
