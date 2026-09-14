package com.wallet.payment.infrastructure.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.wallet.payment.domain.IdempotencyStore;

import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.Response;

/**
 * Infrastructure adapter: IdempotencyStore implementation using Redis.
 * <p>
 * Uses Vert.x Mutiny RedisAPI for raw command execution (SET NX EX, GET).
 * <p>
 * Key pattern: {@code idemp:payment:{endpoint}:{idempotency_key}}
 * States: IN_PROGRESS | COMPLETED | FAILED
 * TTL: configurable, default 24 hours for payments.
 */
@ApplicationScoped
public class IdempotencyStoreAdapter implements IdempotencyStore {

    private static final Logger log = Logger.getLogger(IdempotencyStoreAdapter.class);
    private static final String KEY_PREFIX = "idemp:payment:";

    private final RedisAPI redisAPI;

    @ConfigProperty(name = "wallet.idempotency.ttl-hours", defaultValue = "24")
    int defaultTtlHours;

    @Inject
    public IdempotencyStoreAdapter(RedisAPI redisAPI) {
        this.redisAPI = redisAPI;
    }

    @Override
    public SlotState claim(String key, int ttlHours) {
        String redisKey = KEY_PREFIX + key;
        long ttlSeconds = Duration.ofHours(ttlHours).getSeconds();

        // SET key value EX ttl NX — atomic claim
        Response response = redisAPI.set(List.of(redisKey, "IN_PROGRESS", "EX", String.valueOf(ttlSeconds), "NX"))
                .await().indefinitely();

        if (response != null && "OK".equals(response.toString())) {
            log.debugf("Claimed idempotency slot: key=%s", key);
            return SlotState.IN_PROGRESS;
        }

        // Slot already exists — check its state
        Response getResponse = redisAPI.get(redisKey).await().indefinitely();

        if (getResponse == null) {
            // Key expired between SET and GET — retry claim
            return claim(key, ttlHours);
        }

        String state = getResponse.toString();
        return switch (state) {
            case "COMPLETED" -> SlotState.COMPLETED;
            case "IN_PROGRESS" -> SlotState.DUPLICATE;
            case "FAILED" -> SlotState.FAILED;
            default -> {
                log.warnf("Unknown idempotency state: key=%s, state=%s", key, state);
                yield SlotState.DUPLICATE;
            }
        };
    }

    @Override
    public void complete(String key, String responsePayload, int ttlHours) {
        String redisKey = KEY_PREFIX + key;
        long ttlSeconds = Duration.ofHours(ttlHours).getSeconds();

        redisAPI.set(List.of(redisKey, "COMPLETED:" + responsePayload, "EX", String.valueOf(ttlSeconds)))
                .await().indefinitely();

        log.debugf("Marked idempotency slot COMPLETED: key=%s", key);
    }

    @Override
    public void markFailed(String key) {
        String redisKey = KEY_PREFIX + key;
        long ttlSeconds = Duration.ofHours(defaultTtlHours).getSeconds();

        redisAPI.set(List.of(redisKey, "FAILED", "EX", String.valueOf(ttlSeconds)))
                .await().indefinitely();

        log.debugf("Marked idempotency slot FAILED: key=%s", key);
    }

    @Override
    public Optional<String> getCachedResponse(String key) {
        String redisKey = KEY_PREFIX + key;

        Response response = redisAPI.get(redisKey).await().indefinitely();

        if (response == null) {
            return Optional.empty();
        }

        String value = response.toString();
        if (value.startsWith("COMPLETED:")) {
            return Optional.of(value.substring("COMPLETED:".length()));
        }
        return Optional.empty();
    }
}
