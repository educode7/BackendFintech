package com.wallet.notification.infrastructure.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

/**
 * CORS filter for Notification Service.
 */
@Provider
@PreMatching
public class CorsFilter implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(CorsFilter.class);
    private static final String ALLOWED_ORIGINS = "http://localhost:4200,http://localhost:8080";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String origin = requestContext.getHeaderString("Origin");
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            requestContext.abortWith(Response.ok()
                    .header("Access-Control-Allow-Origin", resolveOrigin(origin))
                    .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH")
                    .header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Correlation-Id")
                    .header("Access-Control-Max-Age", "3600")
                    .build());
        }
    }

    private String resolveOrigin(String origin) {
        if (origin == null) return "*";
        if (ALLOWED_ORIGINS.contains(origin)) return origin;
        return ALLOWED_ORIGINS.split(",")[0];
    }
}
