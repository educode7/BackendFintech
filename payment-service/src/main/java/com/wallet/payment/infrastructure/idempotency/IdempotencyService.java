package com.wallet.payment.infrastructure.idempotency;

import com.wallet.shared.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Redis-backed idempotency for mutation endpoints.
 *
 * Why Redis and not a SQL table:
 * - Sub-millisecond lookups for repeat requests under load.
 * - TTL is trivial and cheap; we don't need long-term auditability.
 * - SET NX is atomic — no race between concurrent first-time requests.
 *
 * Two-phase:
 * 1. Read the slot; if present, replay the cached result.
 * 2. Otherwise attempt to claim it with SET NX EX 86400. First writer wins;
 *    losers see {@code false} and re-fetch the now-cached result.
 *
 * Why 24h TTL: matches typical idempotency windows (Stripe, Square) — long enough
 * for sane client retries, short enough to keep Redis memory bounded.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final StringRedisTemplate redis;

    @Value("${wallet.idempotency.ttl-hours:24}")
    private long ttlHours;

    public <T> IdempotencyResult<T> executeOnce(String key, Class<T> type, Supplier<T> action) {
        String cacheKey = "wallet:idem:" + key;
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("idempotency replay key={}", key);
            return IdempotencyResult.replayed(JsonUtil.fromJson(cached, type));
        }

        Boolean claimed = redis.opsForValue().setIfAbsent(cacheKey, "PENDING", Duration.ofHours(ttlHours));
        if (Boolean.FALSE.equals(claimed)) {
            // Another thread claimed first; let it run, then we replay whatever it cached.
            String result = redis.opsForValue().get(cacheKey);
            return IdempotencyResult.replayed(JsonUtil.fromJson(result, type));
        }

        try {
            T value = action.get();
            redis.opsForValue().set(cacheKey, JsonUtil.toJson(value), Duration.ofHours(ttlHours));
            return IdempotencyResult.executed(value);
        } catch (RuntimeException ex) {
            // Free the slot — the failure wasn't durable, a retry should be able to re-execute.
            redis.delete(cacheKey);
            throw ex;
        }
    }

    public record IdempotencyResult<T>(T value, boolean replayed) {
        public static <T> IdempotencyResult<T> executed(T v) { return new IdempotencyResult<>(v, false); }
        public static <T> IdempotencyResult<T> replayed(T v)  { return new IdempotencyResult<>(v, true); }
    }
}
