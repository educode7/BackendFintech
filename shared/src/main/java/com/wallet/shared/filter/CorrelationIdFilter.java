package com.wallet.shared.filter;

import com.wallet.shared.context.CorrelationContext;
import com.wallet.shared.util.IdGenerator;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

/**
 * Servlet filter that ensures every request has a correlation ID.
 *
 * <p>If the client sent {@code X-Correlation-Id} we honour it (trust the upstream
 * gateway); otherwise we mint a UUID v7 so the ID is also roughly time-sortable
 * in logs.
 *
 * <p>The ID is bound to a {@link ScopedValue} for the lifetime of the request.
 * Anything that runs in the {@code .call(...)} closure (services, repos, Kafka
 * producers) can read it via {@link CorrelationContext#current()} /
 * {@link CorrelationContext#currentOrNull()}. It is inherited across virtual
 * threads and structured concurrency scopes automatically — no
 * {@code ThreadLocal.MDC.put}/{@code remove} bookkeeping.
 *
 * <p>Why not also a {@code MDC.put} for log enrichment: we removed the
 * {@code %X{correlationId}} pattern from {@code application.yml}. Logging
 * statements that need the ID pass it explicitly as a placeholder
 * ({@code "correlationId={}"}) so we don't depend on a ThreadLocal that may
 * not survive virtual-thread switches.
 */
public class CorrelationIdFilter implements Filter {

    public static final String HEADER = "X-Correlation-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String correlationId = Optional.ofNullable(http.getHeader(HEADER))
            .filter(s -> !s.isBlank())
            .orElseGet(IdGenerator::newId);

        // Set the response header BEFORE entering the scope so the client always
        // sees the correlation id we used, even if downstream code throws.
        httpResponse.setHeader(HEADER, correlationId);

        try {
            ScopedValue.where(CorrelationContext.CORRELATION_ID, correlationId)
                .call(() -> {
                    chain.doFilter(request, response);
                    return null;
                });
        } catch (RuntimeException | IOException | ServletException ex) {
            throw ex;
        } catch (Exception ex) {
            // Defensive: Callable declares Exception, but nothing here can produce
            // a checked exception other than IOException/ServletException from
            // the chain.
            throw new IllegalStateException("unexpected checked exception from ScopedValue.call", ex);
        }
    }
}
