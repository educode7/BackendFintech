package com.wallet.payment.infrastructure.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import io.vertx.core.http.HttpHeaders;

/**
 * OWASP Security Headers — response filter.
 * Adds hardened headers to every HTTP response.
 */
@Provider
public class SecurityHeadersResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        var headers = responseContext.getHeaders();

        // Prevent MIME-type sniffing
        headers.putSingle("X-Content-Type-Options", "nosniff");

        // Prevent clickjacking
        headers.putSingle("X-Frame-Options", "DENY");

        // XSS Protection (legacy browsers)
        headers.putSingle("X-XSS-Protection", "1; mode=block");

        // Strict Transport Security (HTTPS only)
        headers.putSingle("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // Content Security Policy
        headers.putSingle("Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'");

        // Referrer Policy
        headers.putSingle("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions Policy
        headers.putSingle("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=()");

        // Cache control for API responses
        headers.putSingle("Cache-Control", "no-store, no-cache, must-revalidate");
        headers.putSingle("Pragma", "no-cache");
    }
}
