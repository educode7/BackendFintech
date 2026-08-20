package com.wallet.shared.filter;

import com.wallet.shared.context.CorrelationContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Structured request logging: one INFO line per request with method, path,
 * status, duration and correlationId.
 *
 * <p>The correlation id is read directly from {@link CorrelationContext} (not
 * from MDC) so the log line is correct even if execution hops between virtual
 * threads.
 */
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String START_ATTR = "wallet.request.startMs";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        HttpServletResponse httpRes = (HttpServletResponse) response;
        long start = System.currentTimeMillis();
        http.setAttribute(START_ATTR, start);

        log.info("request.start method={} path={} correlationId={}",
            http.getMethod(), http.getRequestURI(), CorrelationContext.currentOrNull());

        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            log.info("request.end method={} path={} status={} durationMs={} correlationId={}",
                http.getMethod(),
                http.getRequestURI(),
                httpRes.getStatus(),
                durationMs,
                CorrelationContext.currentOrNull());
        }
    }
}
