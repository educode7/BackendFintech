package com.wallet.gateway.filter;

import com.wallet.shared.util.IdGenerator;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Optional;

/**
 * Reactive variant of {@code CorrelationIdFilter} for the WebFlux gateway.
 * Honours inbound {@code X-Correlation-Id} or mints a UUID v7, echoes it back
 * on the response, and propagates it through the Reactor {@code Context} so
 * downstream filters and outbound WebClient calls can read it.
 *
 * <p>Why Reactor Context and not a {@code ScopedValue}:
 * WebFlux runs on a Netty event loop — execution hops between threads via
 * {@code Mono} continuations and {@code ScopedValue} is bound to a thread
 * stack, not the reactive stream. The canonical primitive here is
 * {@code Mono.contextWrite(...)} + {@code Mono.deferContextual(...)}.
 *
 * <p>The downstream servlet services don't need a bridge because the
 * correlationId already travels over HTTP via the {@code X-Correlation-Id}
 * header — each service's {@code CorrelationIdFilter} re-binds it to a
 * {@code ScopedValue} locally.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdGatewayFilter implements GlobalFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String CONTEXT_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(HEADER))
            .filter(s -> !s.isBlank())
            .orElseGet(IdGenerator::newId);

        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = response.getHeaders();
        if (!headers.containsHeader(HEADER)) {
            headers.add(HEADER, correlationId);
        }

        ServerWebExchange mutated = exchange.mutate()
            .request(r -> r.header(HEADER, correlationId))
            .build();

        return chain.filter(mutated)
            .contextWrite(Context.of(CONTEXT_KEY, correlationId));
    }
}
