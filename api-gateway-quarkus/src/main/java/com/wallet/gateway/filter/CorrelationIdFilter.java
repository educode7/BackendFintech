package com.wallet.gateway.filter;

import java.util.Optional;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.wallet.shared.util.IdGenerator;

/**
 * Reactive Correlation ID filter.
 * Mints UUID v7 or honours inbound X-Correlation-Id.
 * Propagated via JAX-RS headers to downstream services.
 */
@Provider
@PreMatching
@RequestScoped
public class CorrelationIdFilter implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(CorrelationIdFilter.class);
    public static final String HEADER = "X-Correlation-Id";

    @Inject
    jakarta.ws.rs.core.Context context;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String correlationId = Optional.ofNullable(requestContext.getHeaderString(HEADER))
                .filter(s -> !s.isBlank())
                .orElseGet(IdGenerator::newId);

        // Set for downstream propagation
        requestContext.getHeaders().putSingle(HEADER, correlationId);

        log.debugf("CorrelationId: %s %s", requestContext.getMethod(), requestContext.getUriInfo().getRequestUri().getPath());
    }
}
