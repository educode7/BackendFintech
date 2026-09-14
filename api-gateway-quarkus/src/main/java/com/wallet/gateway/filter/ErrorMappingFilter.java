package com.wallet.gateway.filter;

import java.net.URI;
import java.time.Instant;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.wallet.shared.api.ErrorResponse;

/**
 * Maps upstream errors to RFC 9457 Problem+JSON.
 */
@Provider
public class ErrorMappingFilter implements ContainerResponseFilter {

    private static final Logger log = Logger.getLogger(ErrorMappingFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        int status = responseContext.getStatus();
        if (status >= 400) {
            String correlationId = requestContext.getHeaderString(CorrelationIdFilter.HEADER);
            String code = classify(status);
            ErrorResponse error = ErrorResponse.of(code, "Upstream error", status,
                    URI.create("about:blank"), correlationId);
            responseContext.setEntity(error);
            responseContext.getHeaders().putSingle("Content-Type", "application/problem+json");
        }
    }

    private String classify(int status) {
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 422 -> "VALIDATION_ERROR";
            case 429 -> "RATE_LIMITED";
            default -> status >= 500 ? "INTERNAL_ERROR" : "ERROR";
        };
    }
}
