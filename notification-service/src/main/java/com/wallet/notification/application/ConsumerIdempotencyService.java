package com.wallet.notification.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed idempotency for the notification consumer.
 *
 * Why we need it even though the {@code processedEventId} column is UNIQUE:
 * - The DB UNIQUE constraint is reactive (only catches duplicates at insert).
 * - Redis SET NX is proactive — skip the entire handler before doing any work.
 * - Together they form a belt-and-braces guarantee.
 *
 * Why 7 days: notifications are typically less replayed than payments, and
 * we want the marker to outlive any reasonable Kafka retention.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsumerIdempotencyService {

    private final StringRedisTemplate redis;

    @Value("${wallet.consumer-idempotency.ttl-days:7}")
    private long ttlDays;

    public boolean tryClaim(String eventId) {
        String key = "notification:processed:" + eventId;
        Boolean claimed = redis.opsForValue().setIfAbsent(key, "1", Duration.ofDays(ttlDays));
        return Boolean.TRUE.equals(claimed);
    }
}
