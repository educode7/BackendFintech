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

/**
 * Registers gateway proxy routes on the Vert.x Router at startup.
 * Uses a Vert.x BodyHandler to capture the request body before RESTEasy Reactive consumes it,
 * then forwards via Java 25 HttpClient on virtual threads.
 */
@ApplicationScoped
public class GatewayRouteRegistrar {

    private static final Logger log = Logger.getLogger(GatewayRouteRegistrar.class);

    private final HttpClient httpClient;
    private final Vertx vertx;

    @ConfigProperty(name = "wallet.gateway.routes.payment-service.url")
    String paymentServiceUrl;

    @ConfigProperty(name = "wallet.gateway.routes.account-service.url")
    String accountServiceUrl;

    @ConfigProperty(name = "wallet.gateway.routes.notification-service.url")
    String notificationServiceUrl;

    public GatewayRouteRegistrar(Vertx vertx) {
        this.vertx = vertx;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    void onStart(@Observes StartupEvent event, Router router) {

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

        log.infof("Gateway routes registered: accounts→%s, payments→%s, notifications→%s",
                accountServiceUrl, paymentServiceUrl, notificationServiceUrl);
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
        } else {
            // For methods with no body (GET, DELETE, HEAD, OPTIONS), send immediately.
            // Vert.x bodyHandler NEVER fires for GET requests — there is no body to read,
            // so waiting for it causes an infinite hang.
            String method = ctx.request().method().name();
            if ("GET".equals(method) || "DELETE".equals(method)
                    || "HEAD".equals(method) || "OPTIONS".equals(method)) {
                log.infof("No-body method %s, sending directly", method);
                sendProxyRequest(ctx, uri, targetUrl, new byte[0]);
            } else {
                log.warnf("Body is null or empty for %s, falling back to bodyHandler", method);
                ctx.request().bodyHandler(body -> {
                    log.infof("Body via bodyHandler fallback, length=%d", body.length());
                    sendProxyRequest(ctx, uri, targetUrl, body.getBytes());
                });
            }
        }
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
