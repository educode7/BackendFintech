package com.wallet.notification.infrastructure.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

/**
 * Structured request/response logging filter for Notification Service.
 */
@Provider
public class RequestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger log = Logger.getLogger("com.wallet.notification.ACCESS");
    private static final String START_TIME = "request.startTime";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        requestContext.setProperty(START_TIME, System.nanoTime());
        String correlationId = requestContext.getHeaderString("X-Correlation-Id");
        log.infof("REQ %s %s correlationId=%s remote=%s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri().getPath(),
                correlationId != null ? correlationId : "none",
                requestContext.getHeaderString("X-Forwarded-For"));
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Long startTime = (Long) requestContext.getProperty(START_TIME);
        long durationMs = startTime != null ? (System.nanoTime() - startTime) / 1_000_000 : -1;
        String correlationId = requestContext.getHeaderString("X-Correlation-Id");

        log.infof("RES %s %s status=%d durationMs=%d correlationId=%s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri().getPath(),
                responseContext.getStatus(),
                durationMs,
                correlationId != null ? correlationId : "none");
    }
}
