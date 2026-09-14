package com.wallet.account.infrastructure.observability;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.runtime.StartupEvent;

/**
 * Business metrics for Account Service.
 * <p>
 * Metrics:
 * - account.opened.total (counter)
 * - account.deposit.total (counter)
 * - account.withdrawal.total (counter)
 * - account.withdrawal.insufficient_funds.total (counter)
 * - account.balance (gauge per account)
 * - account.event.replay.duration (timer)
 */
@ApplicationScoped
public class AccountMetrics {

    private final MeterRegistry registry;

    private Counter accountOpenedCounter;
    private Counter depositCounter;
    private Counter withdrawalCounter;
    private Counter insufficientFundsCounter;
    private Timer eventReplayTimer;
    private DistributionSummary balanceSummary;

    @Inject
    public AccountMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    void onStart(@Observes StartupEvent event) {
        accountOpenedCounter = Counter.builder("account.opened.total")
                .description("Total accounts opened")
                .register(registry);

        depositCounter = Counter.builder("account.deposit.total")
                .description("Total deposits")
                .register(registry);

        withdrawalCounter = Counter.builder("account.withdrawal.total")
                .description("Total withdrawals")
                .register(registry);

        insufficientFundsCounter = Counter.builder("account.withdrawal.insufficient_funds.total")
                .description("Withdrawals rejected due to insufficient funds")
                .register(registry);

        eventReplayTimer = Timer.builder("account.event.replay.duration")
                .description("Event replay latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        balanceSummary = DistributionSummary.builder("account.balance")
                .description("Account balance distribution")
                .baseUnit("currency_units")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void recordAccountOpened() { accountOpenedCounter.increment(); }
    public void recordDeposit(java.math.BigDecimal amount) { depositCounter.increment(); balanceSummary.record(amount.doubleValue()); }
    public void recordWithdrawal(java.math.BigDecimal amount) { withdrawalCounter.increment(); }
    public void recordInsufficientFunds() { insufficientFundsCounter.increment(); }
    public Timer.Sample startReplay() { return Timer.start(registry); }
    public void stopReplay(Timer.Sample sample) { sample.stop(eventReplayTimer); }
}
