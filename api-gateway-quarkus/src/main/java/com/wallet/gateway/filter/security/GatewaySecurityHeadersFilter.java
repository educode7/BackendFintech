package com.wallet.gateway.filter.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * OWASP Security Headers — response filter for API Gateway.
 * First line of defense; all responses carry hardened headers.
 */
@Provider
public class GatewaySecurityHeadersFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        var headers = responseContext.getHeaders();
        headers.putSingle("X-Content-Type-Options", "nosniff");
        headers.putSingle("X-Frame-Options", "DENY");
        headers.putSingle("X-XSS-Protection", "1; mode=block");
        headers.putSingle("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        headers.putSingle("Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'");
        headers.putSingle("Referrer-Policy", "strict-origin-when-cross-origin");
        headers.putSingle("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=()");
        headers.putSingle("Cache-Control", "no-store, no-cache, must-revalidate");
        headers.putSingle("Pragma", "no-cache");

        // Remove server identification headers
        headers.remove("Server");
        headers.remove("X-Powered-By");
    }
}
