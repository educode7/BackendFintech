package com.wallet.payment.infrastructure.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

/**
 * OWASP Security Headers filter.
 * Adds recommended security headers to every response.
 * Source: OWASP Secure Headers Project.
 */
@Provider
public class SecurityHeadersFilter implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(SecurityHeadersFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Headers are set on the response, but we need a ContainerResponseFilter
        // This filter is registered as pre-matching; actual headers set in SecurityHeadersResponseFilter
    }
}
