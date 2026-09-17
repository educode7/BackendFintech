package com.wallet.auth.interfaces.rest.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

/**
 * CORS filter for Auth Service.
 * Handles preflight OPTIONS and adds CORS headers to all responses.
 * Supports credentials (HttpOnly cookies) for refresh token flow.
 */
@Provider
@PreMatching
public class AuthCorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger log = Logger.getLogger(AuthCorsFilter.class);
    private static final String ALLOWED_ORIGINS = "http://localhost:4200,http://localhost:8080";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            String origin = requestContext.getHeaderString("Origin");
            requestContext.abortWith(Response.ok()
                    .header("Access-Control-Allow-Origin", resolveOrigin(origin))
                    .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH")
                    .header("Access-Control-Allow-Headers",
                            "Content-Type, Authorization, X-Correlation-Id")
                    .header("Access-Control-Expose-Headers", "X-Correlation-Id, Location")
                    .header("Access-Control-Allow-Credentials", "true")
                    .header("Access-Control-Max-Age", "3600")
                    .build());
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String origin = requestContext.getHeaderString("Origin");
        if (origin != null) {
            responseContext.getHeaders().putSingle("Access-Control-Allow-Origin", resolveOrigin(origin));
            responseContext.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
            responseContext.getHeaders().putSingle("Access-Control-Expose-Headers", "X-Correlation-Id");
        }
    }

    private String resolveOrigin(String origin) {
        if (origin == null) return "*";
        if (ALLOWED_ORIGINS.contains(origin)) return origin;
        return ALLOWED_ORIGINS.split(",")[0];
    }
}
