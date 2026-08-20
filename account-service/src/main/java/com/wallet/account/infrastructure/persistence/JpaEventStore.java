package com.wallet.account.infrastructure.persistence;

import com.wallet.account.domain.Account;
import com.wallet.account.domain.exception.AccountNotFoundException;
import com.wallet.shared.context.CorrelationContext;
import com.wallet.shared.event.AccountEvent;
import com.wallet.shared.event.AccountOpenedEvent;
import com.wallet.shared.event.MoneyDepositedEvent;
import com.wallet.shared.event.MoneyWithdrawnEvent;
import com.wallet.shared.util.IdGenerator;
import com.wallet.shared.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

/**
 * Application-level event store wrapping the JPA repository.
 *
 * <h2>Concurrency model</h2>
 * Concurrent deposits/withdrawals on the same account race the version counter.
 * We use optimistic locking: the UNIQUE constraint on (aggregate_id, version)
 * is the authoritative guard. On conflict we retry with <b>exponential backoff</b>
 * (1ms → 2ms → 4ms) to avoid CPU-burning spin loops under contention.
 *
 * <h2>Why not SELECT ... FOR UPDATE?</h2>
 * Pessimistic locking serializes all writes to the same aggregate, which kills
 * throughput for hot accounts. Optimistic locking + bounded retry is better:
 * under normal load (low contention) the first attempt always wins; under
 * contention the backoff reduces retry pressure on the DB.
 *
 * <h2>Virtual threads</h2>
 * All blocking calls (JPA save, query) run on virtual threads (Spring Boot 4
 * {@code spring.threads.virtual.enabled=true}). Virtual threads are cheap to
 * park during backoff, so the exponential sleep doesn't waste platform threads.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class JpaEventStore {

    private static final int MAX_RETRIES = 3;

    private final EventStoreJpaRepository repository;

    /**
     * Appends an event with optimistic-locking semantics.
     *
     * <p>On DataIntegrityViolationException (version conflict), we:
     * <ol>
     *   <li>Apply exponential backoff: {@code 1ms * 2^attempt} (1, 2, 4 ms).</li>
     *   <li>Use {@link Thread#onSpinWait()} to hint the CPU we're in a spin-wait
     *       (reduces power consumption and frees pipeline slots on ARM/x86).</li>
     *   <li>Re-query the next version (the winning transaction has incremented it).</li>
     * </ol>
     */
    public void append(AccountEvent event, String aggregateType) {
        int attempt = 0;
        while (true) {
            try {
                EventStoreEntity row = new EventStoreEntity();
                row.setId(IdGenerator.newId());
                row.setAggregateId(event.accountId());
                row.setAggregateType(aggregateType);
                row.setEventType(event.getClass().getSimpleName());
                row.setEventData(JsonUtil.toJson(event));
                row.setVersion(nextVersion(event.accountId()));
                row.setCreatedAt(Instant.now());
                row.setCorrelationId(CorrelationContext.currentOrNull());
                repository.save(row);
                return;
            } catch (DataIntegrityViolationException ex) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    throw new com.wallet.account.domain.exception.ConcurrentModificationException(event.accountId());
                }
                // Exponential backoff: 1ms, 2ms, 4ms — avoids CPU-burning spin loops.
                // Virtual threads park efficiently during sleep; no platform thread is blocked.
                long backoffMs = 1L << attempt; // 2^1=2, 2^2=4
                log.warn("event append conflict accountId={} attempt={} backoffMs={}",
                    event.accountId(), attempt, backoffMs);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("interrupted during event append retry", e);
                }
                Thread.onSpinWait(); // JDK 9+ CPU hint: reduces pipeline stalls
            }
        }
    }

    /**
     * Replays events for the given aggregate and rebuilds the Account state.
     *
     * <p>Uses Java 24+ {@link Stream Gatherers#scan} to emit every intermediate
     * state as the event stream is folded into the aggregate. This is lazy and
     * memory-efficient: only one Account instance is held at a time.
     */
    public Account load(String accountId) {
        List<EventStoreEntity> rows = repository.findByAggregateIdOrderByVersionAsc(accountId);
        if (rows.isEmpty()) {
            throw new AccountNotFoundException(accountId);
        }
        Stream<AccountEvent> events = rows.stream().map(this::deserialize);
        return events.gather(Gatherers.scan(
                Account::new,
                (acc, event) -> { acc.apply(event); return acc; }
            ))
            .reduce((first, last) -> last)
            .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    public boolean exists(String accountId) {
        return !repository.findByAggregateIdOrderByVersionAsc(accountId).isEmpty();
    }

    /**
     * Returns the next version for the aggregate.
     *
     * <p>Loads all events for the aggregate to find the max version. Under low
     * contention this is fine (typically &lt;100 events per account). For high-churn
     * aggregates (thousands of events), add a {@code MAX(version)} SQL query to
     * the repository.
     */
    private long nextVersion(String aggregateId) {
        List<EventStoreEntity> rows = repository.findByAggregateIdOrderByVersionAsc(aggregateId);
        return rows.isEmpty() ? 1L : rows.get(rows.size() - 1).getVersion() + 1;
    }

    private AccountEvent deserialize(EventStoreEntity row) {
        return switch (row.getEventType()) {
            case "AccountOpenedEvent"  -> JsonUtil.fromJson(row.getEventData(), AccountOpenedEvent.class);
            case "MoneyDepositedEvent" -> JsonUtil.fromJson(row.getEventData(), MoneyDepositedEvent.class);
            case "MoneyWithdrawnEvent" -> JsonUtil.fromJson(row.getEventData(), MoneyWithdrawnEvent.class);
            default -> throw new IllegalStateException("unknown event type: " + row.getEventType());
        };
    }
}
