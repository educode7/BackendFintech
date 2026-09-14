package com.wallet.gateway.observability;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.RedisAPI;

/**
 * Health checks for API Gateway.
 */
@ApplicationScoped
public class GatewayHealthCheck {

    private final RedisAPI redisAPI;

    public GatewayHealthCheck(RedisAPI redisAPI) {
        this.redisAPI = redisAPI;
    }

    @Readiness
    public Uni<HealthCheckResponse> checkRedis() {
        return redisAPI.ping(List.of())
                .onItem().transform(response -> HealthCheckResponse.named("gateway-redis")
                        .up().withData("redis", "connected").build())
                .onFailure().recoverWithItem(HealthCheckResponse.named("gateway-redis")
                        .down().withData("error", "Redis unreachable").build());
    }

    @Readiness
    public Uni<HealthCheckResponse> checkBackendServices() {
        return Uni.createFrom().item(HealthCheckResponse.named("gateway-backends")
                .up()
                .withData("payment-service", "http://localhost:8081")
                .withData("account-service", "http://localhost:8082")
                .withData("notification-service", "http://localhost:8083")
                .build());
    }

    @Liveness
    public Uni<HealthCheckResponse> checkLiveness() {
        return Uni.createFrom().item(HealthCheckResponse.named("gateway-liveness")
                .up()
                .withData("pid", ProcessHandle.current().pid())
                .build());
    }
}
