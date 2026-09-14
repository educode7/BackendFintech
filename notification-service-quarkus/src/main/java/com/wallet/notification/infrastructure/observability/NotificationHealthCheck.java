package com.wallet.notification.infrastructure.observability;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.RedisAPI;

/**
 * Custom health checks for Notification Service.
 */
@ApplicationScoped
public class NotificationHealthCheck {

    private final RedisAPI redisAPI;

    @Inject
    public NotificationHealthCheck(RedisAPI redisAPI) {
        this.redisAPI = redisAPI;
    }

    @Readiness
    public Uni<HealthCheckResponse> checkDatabase() {
        return Uni.createFrom().item(HealthCheckResponse.named("notification-database")
                .up()
                .withData("database", "postgresql")
                .build());
    }

    @Readiness
    public Uni<HealthCheckResponse> checkKafkaConsumer() {
        return Uni.createFrom().item(HealthCheckResponse.named("notification-kafka-consumer")
                .up()
                .withData("topic", "payment.events")
                .withData("consumer", "notification-service")
                .build());
    }

    @Readiness
    public Uni<HealthCheckResponse> checkRedis() {
        return redisAPI.ping(List.of())
                .onItem().transformToUni(pong -> Uni.createFrom().item(
                        HealthCheckResponse.named("notification-redis")
                                .up()
                                .withData("redis", "connected")
                                .build()))
                .onFailure().recoverWithItem(
                        HealthCheckResponse.named("notification-redis")
                                .down()
                                .withData("redis", "unreachable")
                                .build());
    }

    @Liveness
    public Uni<HealthCheckResponse> checkLiveness() {
        return Uni.createFrom().item(HealthCheckResponse.named("notification-liveness")
                .up()
                .withData("pid", ProcessHandle.current().pid())
                .build());
    }
}
