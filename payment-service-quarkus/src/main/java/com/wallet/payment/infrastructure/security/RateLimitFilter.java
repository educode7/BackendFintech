package com.wallet.payment.infrastructure.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;



/**
 * Rate limiting filter — applies per-IP token bucket.
 * Configurable via wallet.rate-limit.* properties.
 */
@Provider
@PreMatching
public class RateLimitFilter implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(RateLimitFilter.class);
    private static final int MAX_TOKENS = 100;
    private static final double REFILL_RATE = 10.0; // tokens per second

    private final RateLimiter rateLimiter;

    public RateLimitFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Skip rate limiting for health checks
        String path = requestContext.getUriInfo().getRequestUri().getPath();
        if (path.startsWith("/q/health") || path.startsWith("/q/openapi")) {
            return;
        }

        String clientIp = requestContext.getHeaderString("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = requestContext.getHeaderString("X-Real-IP");
        }
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = "unknown";
        }

        String key = clientIp + ":" + requestContext.getMethod();
        if (!rateLimiter.isAllowed(key, MAX_TOKENS, REFILL_RATE)) {
            log.warnf("Rate limit exceeded for %s %s", clientIp, requestContext.getMethod());
            requestContext.abortWith(Response.status(429)
                    .header("Retry-After", "1")
                    .entity(new com.wallet.shared.api.ErrorResponse(
                            java.net.URI.create("about:blank"),
                            "Rate limit exceeded",
                            429,
                            "Too many requests. Please retry after 1 second.",
                            null,
                            "RATE_LIMITED",
                            null,
                            java.time.Instant.now()))
                    .build());
        }
    }
}
