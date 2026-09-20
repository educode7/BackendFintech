package com.wallet.auth.infrastructure;

import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * CDI producer for {@link WebClient}.
 * Quarkus does not auto-produce this bean — it must be created explicitly.
 */
@ApplicationScoped
public class WebClientProducer {

    @Produces
    @ApplicationScoped
    public WebClient produceWebClient(io.vertx.mutiny.core.Vertx vertx) {
        return WebClient.create(vertx);
    }
}
