package com.wallet.gateway.filter;

import com.wallet.shared.api.ErrorResponse;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Converts upstream error responses into our RFC 9457 envelope so the client
 * always sees the same JSON shape regardless of which service produced the error.
 *
 * Why a gateway filter: services emit their own ProblemDetail; we want a
 * single canonical contract visible to the outside world.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ErrorMappingGatewayFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        response.beforeCommit(() ->
            Mono.deferContextual(ctxView -> {
                int status = response.getStatusCode() != null ? response.getStatusCode().value() : 500;
                if (status >= 400 && !response.getHeaders().containsHeader("Content-Length")) {
                    String correlationId = ctxView.getOrDefault(CorrelationIdGatewayFilter.CONTEXT_KEY, "");
                    ErrorResponse body = ErrorResponse.of(
                        classify(status),
                        "Upstream error",
                        status,
                        URI.create("about:blank"),
                        correlationId
                    );
                    byte[] bytes = ("{\"code\":\"" + body.code() + "\",\"status\":" + body.status()
                        + ",\"detail\":\"" + body.detail() + "\",\"correlationId\":\""
                        + correlationId + "\"}").getBytes();
                    response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
                    DataBuffer buffer = response.bufferFactory().wrap(bytes);
                    return response.writeWith(Mono.just(buffer));
                }
                return Mono.empty();
            })
        );
        return chain.filter(exchange);
    }

    private static String classify(int status) {
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 422 -> "UNPROCESSABLE_ENTITY";
            case 429 -> "TOO_MANY_REQUESTS";
            default -> status >= 500 ? "INTERNAL_ERROR" : "ERROR";
        };
    }
}
