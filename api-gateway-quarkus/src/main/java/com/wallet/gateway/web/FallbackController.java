package com.wallet.gateway.web;

import java.net.URI;
import java.time.Instant;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.wallet.shared.api.ErrorResponse;

/**
 * Fallback endpoints for circuit breaker OPEN state.
 */
@Path("/fallback")
public class FallbackController {

    @GET
    @Path("/payment")
    @Produces(MediaType.APPLICATION_JSON)
    public Response payment() {
        return build("paymentService", "Payment service unavailable, please retry shortly.");
    }

    @GET
    @Path("/account")
    @Produces(MediaType.APPLICATION_JSON)
    public Response account() {
        return build("accountService", "Account service unavailable, please retry shortly.");
    }

    @GET
    @Path("/notification")
    @Produces(MediaType.APPLICATION_JSON)
    public Response notification() {
        return build("notificationService", "Notification service unavailable, please retry shortly.");
    }

    private Response build(String instance, String detail) {
        ErrorResponse error = ErrorResponse.of("CIRCUIT_OPEN", detail, 503,
                URI.create("about:blank"), null);
        return Response.status(503).entity(error).build();
    }
}
