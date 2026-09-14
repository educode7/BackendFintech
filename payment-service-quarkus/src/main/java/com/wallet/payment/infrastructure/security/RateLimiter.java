package com.wallet.payment.infrastructure.security;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.Response;

/**
 * Token-bucket rate limiter backed by Redis.
 * Configured per-endpoint via wallet.rate-limit.* properties.
 */
@ApplicationScoped
public class RateLimiter {

    private static final Logger log = Logger.getLogger(RateLimiter.class);

    private final RedisAPI redisAPI;

    @Inject
    public RateLimiter(RedisAPI redisAPI) {
        this.redisAPI = redisAPI;
    }

    /**
     * Check if request is allowed under token bucket.
     *
     * @param key         unique key (e.g., IP + endpoint)
     * @param maxTokens   burst capacity
     * @param refillRate  tokens per second
     * @return true if allowed, false if rate limited
     */
    public boolean isAllowed(String key, int maxTokens, double refillRate) {
        try {
            String redisKey = "ratelimit:" + key;
            Response response = redisAPI.eval(List.of(
                    buildLuaScript(),
                    "1",
                    redisKey,
                    String.valueOf(maxTokens),
                    String.valueOf(refillRate),
                    String.valueOf(System.currentTimeMillis() / 1000)
            )).await().indefinitely();

            return response != null && response.toLong() == 1;
        } catch (Exception e) {
            log.warnf("Rate limiter failed, allowing request: %s", e.getMessage());
            return true; // fail open
        }
    }

    private String buildLuaScript() {
        return """
            local key = KEYS[1]
            local max_tokens = tonumber(ARGV[1])
            local refill_rate = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])

            local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
            local tokens = tonumber(bucket[1]) or max_tokens
            local last_refill = tonumber(bucket[2]) or now

            local elapsed = now - last_refill
            local new_tokens = math.min(max_tokens, tokens + elapsed * refill_rate)

            if new_tokens >= 1 then
                redis.call('HMSET', key, 'tokens', tostring(new_tokens - 1), 'last_refill', tostring(now))
                redis.call('EXPIRE', key, math.ceil(max_tokens / refill_rate) + 10)
                return 1
            else
                redis.call('HMSET', key, 'tokens', tostring(new_tokens), 'last_refill', tostring(now))
                redis.call('EXPIRE', key, math.ceil(max_tokens / refill_rate) + 10)
                return 0
            end
            """;
    }
}
