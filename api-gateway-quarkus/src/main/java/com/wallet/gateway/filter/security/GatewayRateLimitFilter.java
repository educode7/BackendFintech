package com.wallet.gateway.filter.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.Request;

/**
 * Token-bucket rate limiter for the API Gateway.
 * Protects all backend services from traffic spikes.
 */
@Provider
@PreMatching
public class GatewayRateLimitFilter implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(GatewayRateLimitFilter.class);
    private static final int MAX_TOKENS = 200;
    private static final double REFILL_RATE = 20.0;

    private final Redis redis;

    public GatewayRateLimitFilter(Redis redis) {
        this.redis = redis;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getRequestUri().getPath();
        if (path.startsWith("/q/") || path.startsWith("/fallback/")) {
            return;
        }

        String clientIp = getClientIp(requestContext);
        String key = "gw:" + clientIp;

        if (!isAllowed(key)) {
            log.warnf("Gateway rate limit exceeded for %s", clientIp);
            requestContext.abortWith(Response.status(429)
                    .header("Retry-After", "1")
                    .header("Content-Type", "application/problem+json")
                    .entity("{\"type\":\"about:blank\",\"title\":\"Rate limit exceeded\",\"status\":429,\"detail\":\"Too many requests. Please retry after 1 second.\"}")
                    .build());
        }
    }

    private boolean isAllowed(String key) {
        try {
            var redisResponse = redis
                    .send(Request.cmd(Command.EVAL)
                    .arg(LUA_SCRIPT)
                    .arg("1")
                    .arg(key)
                    .arg(String.valueOf(MAX_TOKENS))
                    .arg(String.valueOf(REFILL_RATE))
                    .arg(String.valueOf(System.currentTimeMillis() / 1000)))
                    .await()
                    .indefinitely();
            return redisResponse != null && Long.parseLong(redisResponse.toString()) == 1;
        } catch (Exception e) {
            log.warnf("Rate limiter Redis failed, allowing request: %s", e.getMessage());
            return true;
        }
    }

    private String getClientIp(ContainerRequestContext requestContext) {
        String xff = requestContext.getHeaderString("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = requestContext.getHeaderString("X-Real-IP");
        return (realIp != null && !realIp.isBlank()) ? realIp : "unknown";
    }

    private static final String LUA_SCRIPT = """
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
