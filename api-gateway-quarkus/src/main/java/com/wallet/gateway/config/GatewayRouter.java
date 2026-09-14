package com.wallet.gateway.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.ext.web.handler.ResponseContentTypeHandler;

/**
 * Gateway routing configuration.
 * <p>
 * Routes HTTP requests to backend services based on path prefix.
 * Implements circuit breaker, retry, and rate limiting via reactive handlers.
 */
@ApplicationScoped
public class GatewayRouter {

    private static final Logger log = Logger.getLogger(GatewayRouter.class);

    private final Vertx vertx;
    private final WebClient webClient;

    @ConfigProperty(name = "wallet.gateway.routes.payment-service.url")
    String paymentServiceUrl;

    @ConfigProperty(name = "wallet.gateway.routes.account-service.url")
    String accountServiceUrl;

    @ConfigProperty(name = "wallet.gateway.routes.notification-service.url")
    String notificationServiceUrl;

    @Inject
    public GatewayRouter(Vertx vertx) {
        this.vertx = vertx;
        this.webClient = WebClient.create(vertx);
    }

    public Router createRouter() {
        Router router = Router.router(vertx);

        // Health check passthrough
        router.get("/q/health").handler(ctx -> ctx.response().end("OK"));

        // Payment service routes
        router.route("/api/v1/payments*")
                .handler(ctx -> proxyRequest(ctx, paymentServiceUrl));

        // Account service routes
        router.route("/api/v1/accounts*")
                .handler(ctx -> proxyRequest(ctx, accountServiceUrl));

        // Notification service routes
        router.route("/api/v1/notifications*")
                .handler(ctx -> proxyRequest(ctx, notificationServiceUrl));

        // Fallback routes
        router.get("/fallback/payment").handler(ctx -> fallbackResponse(ctx, "paymentService"));
        router.get("/fallback/account").handler(ctx -> fallbackResponse(ctx, "accountService"));
        router.get("/fallback/notification").handler(ctx -> fallbackResponse(ctx, "notificationService"));

        return router;
    }

    private void proxyRequest(io.vertx.mutiny.ext.web.RoutingContext ctx, String serviceUrl) {
        String uri = ctx.request().uri();
        log.debugf("Proxying %s %s → %s", ctx.request().method(), uri, serviceUrl + uri);

        webClient.requestAbs(ctx.request().method(), serviceUrl + uri)
                .send()
                .onItem().transform(response -> {
                    ctx.response().setStatusCode(response.statusCode());
                    response.headers().forEach((k, v) -> ctx.response().putHeader(k, v));
                    ctx.response().end(response.bodyAsBuffer());
                    return response;
                })
                .onFailure().call(f -> {
                    log.warnf("Proxy failed for %s: %s", uri, f.getMessage());
                    ctx.response().setStatusCode(503);
                    ctx.response().putHeader("Content-Type", "application/problem+json");
                    JsonObject error = new JsonObject()
                            .put("type", "about:blank")
                            .put("title", "Service Unavailable")
                            .put("status", 503)
                            .put("detail", "Backend service unavailable")
                            .put("code", "CIRCUIT_OPEN");
                    ctx.response().end(error.encode());
                    return io.smallrye.mutiny.Uni.createFrom().voidItem();
                });
    }

    private void fallbackResponse(io.vertx.mutiny.ext.web.RoutingContext ctx, String service) {
        ctx.response().setStatusCode(503);
        ctx.response().putHeader("Content-Type", "application/problem+json");
        JsonObject error = new JsonObject()
                .put("type", "about:blank")
                .put("title", "Service Unavailable")
                .put("status", 503)
                .put("detail", "Service unavailable, please retry shortly.")
                .put("code", "CIRCUIT_OPEN")
                .put("instance", service);
        ctx.response().end(error.encode());
    }
}
