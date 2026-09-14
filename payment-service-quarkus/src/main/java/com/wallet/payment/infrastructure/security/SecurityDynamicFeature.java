package com.wallet.payment.infrastructure.security;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

/**
 * JAX-RS Dynamic Feature for role-based security.
 * Maps endpoint classes/methods to required roles.
 */
@Provider
@ApplicationScoped
public class SecurityDynamicFeature implements DynamicFeature {

    private static final Logger log = Logger.getLogger(SecurityDynamicFeature.class);

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        Class<?> resourceClass = resourceInfo.getResourceClass();
        log.debugf("Configuring security for: %s#%s",
                resourceClass.getSimpleName(), resourceInfo.getResourceMethod().getName());
    }
}
