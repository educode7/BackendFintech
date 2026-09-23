package com.wallet.gateway.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;

/**
 * Registers gateway proxy routes on the Vert.x Router at startup.
 * Uses a Vert.x BodyHandler to capture the request body before RESTEasy Reactive consumes it,
 * then forwards via Java 25 HttpClient on virtual threads.
 */
@ApplicationScoped
public class GatewayRouteRegistrar {

    private static final Logger log = Logger.getLogger(GatewayRouteRegistrar.class);

    /**
     * Injects W3C {@code traceparent}/{@code tracestate} into the outbound
     * {@link HttpRequest.Builder}. Raw {@code java.net.http.HttpClient} is not
     * auto-instrumented by Quarkus OpenTelemetry, so without this the gateway
     * starts a root span but the backend opens a disconnected trace.
     */
    private static final TextMapSetter<HttpRequest.Builder> OTEL_HEADER_SETTER =
            (builder, key, value) -> {
                if (key != null && value != null) {
                    builder.header(key, value);
                }
            };

    private final HttpClient httpClient;
    private final Vertx vertx;

    @ConfigProperty(name = "wallet.gateway.routes.payment-service.url")
    String paymentServiceUrl;

    @ConfigProperty(name = "wallet.gateway.routes.account-service.url")
    String accountServiceUrl;

    @ConfigProperty(name = "wallet.gateway.routes.notification-service.url")
    String notificationServiceUrl;

    @ConfigProperty(name = "wallet.gateway.routes.auth-service.url")
    String authServiceUrl;

    public GatewayRouteRegistrar(Vertx vertx) {
        this.vertx = vertx;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    void onStart(@Observes StartupEvent event, Router router) {

        // CORS handler — must run BEFORE proxy routes
        router.route().order(-2).handler(ctx -> {
            String origin = ctx.request().getHeader("Origin");
            if (origin != null) {
                ctx.response().putHeader("Access-Control-Allow-Origin", origin);
                ctx.response().putHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
                ctx.response().putHeader("Access-Control-Allow-Headers",
                        "Content-Type, Authorization, X-Request-Id, Idempotency-Key, X-Correlation-Id, traceparent, tracestate, X-Client-Version, X-Client-Platform");
                ctx.response().putHeader("Access-Control-Allow-Credentials", "true");
                ctx.response().putHeader("Access-Control-Max-Age", "3600");
            }

            // Handle preflight OPTIONS — respond immediately
            if ("OPTIONS".equalsIgnoreCase(ctx.request().method().name())) {
                ctx.response().setStatusCode(204);
                ctx.response().end();
                return;
            }

            ctx.next();
        });

        // CRITICAL: Register a global BodyHandler BEFORE RESTEasy Reactive's JAX-RS filter chain.
        // Without this, the @PreMatching JAX-RS CorrelationIdFilter consumes the request body
        // and ctx.body().buffer() returns null, bodyHandler never fires.
        router.route().order(-1).handler(BodyHandler.create());

        router.route("/api/v1/payments*")
                .handler(ctx -> proxyRequest(ctx, paymentServiceUrl));
        router.route("/api/v1/accounts*")
                .handler(ctx -> proxyRequest(ctx, accountServiceUrl));
        router.route("/api/v1/notifications*")
                .handler(ctx -> proxyRequest(ctx, notificationServiceUrl));
        router.route("/api/v1/auth*")
                .handler(ctx -> proxyRequest(ctx, authServiceUrl));

        log.infof("Gateway routes registered: accounts→%s, payments→%s, notifications→%s, auth→%s",
                accountServiceUrl, paymentServiceUrl, notificationServiceUrl, authServiceUrl);
    }

    private void proxyRequest(io.vertx.ext.web.RoutingContext ctx, String serviceUrl) {
        String uri = ctx.request().uri();
        String targetUrl = serviceUrl + uri;
        log.infof("Proxying %s %s → %s", ctx.request().method(), uri, targetUrl);

        // With BodyHandler registered at order -1, ctx.body().buffer() should now have the body
        io.vertx.core.buffer.Buffer vertxBuf = null;
        try {
            vertxBuf = ctx.body().buffer();
        } catch (Exception e) {
            log.warnf("ctx.body().buffer() failed: %s", e.getMessage());
        }

        if (vertxBuf != null && vertxBuf.length() > 0) {
            log.infof("Body available via ctx.body(), length=%d", vertxBuf.length());
            sendProxyRequest(ctx, uri, targetUrl, vertxBuf.getBytes());
            return;
        }

        // BodyHandler at order -1 already ran. A null/empty buffer means there is
        // no request body (GET, PATCH /read, DELETE, …). Do NOT re-register
        // request.bodyHandler here — the stream is already consumed and Vert.x
        // throws IllegalStateException("Request has already been read") → HTTP 500.
        log.infof("No request body for %s, sending with empty body", ctx.request().method().name());
        sendProxyRequest(ctx, uri, targetUrl, new byte[0]);
    }

    private void sendProxyRequest(io.vertx.ext.web.RoutingContext ctx, String uri,
                                  String targetUrl, byte[] bodyBytes) {
        log.infof("sendProxyRequest: bodyLen=%d, target=%s", bodyBytes.length, targetUrl);

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofSeconds(10));

        // Forward headers (skip restricted/system headers that Java HttpClient manages)
        ctx.request().headers().forEach(entry -> {
            String key = entry.getKey();
            String lower = key.toLowerCase();
            if (!"host".equals(lower) && !"connection".equals(lower)
                    && !"content-length".equals(lower)
                    && !"expect".equals(lower) && !"transfer-encoding".equals(lower)) {
                reqBuilder.header(key, entry.getValue());
            }
        });

        // After copying inbound headers, inject the gateway's active span so the
        // backend continues the SAME trace (overrides any inbound traceparent).
        GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(Context.current(), reqBuilder, OTEL_HEADER_SETTER);

        // Set method and body
        String method = ctx.request().method().name();
        if (bodyBytes.length > 0) {
            reqBuilder.method(method, HttpRequest.BodyPublishers.ofByteArray(bodyBytes));
        } else {
            reqBuilder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        Thread.startVirtualThread(() -> {
            try {
                log.infof("Virtual thread: sending %s to %s (bodyLen=%d)", method, targetUrl, bodyBytes.length);
                HttpResponse<byte[]> response = httpClient.send(reqBuilder.build(),
                        HttpResponse.BodyHandlers.ofByteArray());
                log.infof("Virtual thread: got %d from %s", response.statusCode(), targetUrl);

                vertx.runOnContext(v -> {
                    ctx.response().setStatusCode(response.statusCode());
                    response.headers().map().forEach((k, values) -> {
                        // Skip pseudo-headers and hop-by-hop headers
                        if (!k.startsWith(":") && !"connection".equalsIgnoreCase(k)
                                && !"transfer-encoding".equalsIgnoreCase(k)) {
                            ctx.response().putHeader(k, values);
                        }
                    });
                    ctx.response().end(io.vertx.core.buffer.Buffer.buffer(response.body()));
                });
            } catch (Exception e) {
                log.warnf("Proxy FAILED %s → %s: %s", uri, targetUrl, e.getClass().getSimpleName() + ": " + e.getMessage());
                vertx.runOnContext(v -> sendUnavailable(ctx));
            }
        });
    }

    private void sendUnavailable(io.vertx.ext.web.RoutingContext ctx) {
        if (!ctx.response().ended()) {
            ctx.response().setStatusCode(503);
            ctx.response().putHeader("Content-Type", "application/problem+json");
            JsonObject error = new JsonObject()
                    .put("type", "about:blank")
                    .put("title", "Service Unavailable")
                    .put("status", 503)
                    .put("detail", "Backend service unavailable")
                    .put("code", "CIRCUIT_OPEN");
            ctx.response().end(error.encode());
        }
    }
}
