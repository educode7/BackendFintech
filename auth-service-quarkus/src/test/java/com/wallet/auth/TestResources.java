package com.wallet.auth;

import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * Test resource — produces a WebClient bean for CDI injection in tests.
 * Quarkus ArC validates ALL beans at startup, including adapters that inject WebClient.
 * Without this producer, the test CDI container fails with "Unsatisfied dependency for WebClient".
 */
@ApplicationScoped
public class TestResources {

    @Produces
    @ApplicationScoped
    WebClient produceWebClient(Vertx vertx) {
        return WebClient.create(vertx);
    }
}
