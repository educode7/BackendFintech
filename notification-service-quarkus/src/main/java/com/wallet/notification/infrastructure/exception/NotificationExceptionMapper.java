package com.wallet.notification.infrastructure.exception;

import java.net.URI;
import java.time.Instant;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.wallet.shared.api.ErrorResponse;

/**
 * Global exception mapper for Notification Service.
 */
@Provider
public class NotificationExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException exception) {
        return switch (exception) {
            case IllegalArgumentException e -> buildResponse(Response.Status.BAD_REQUEST,
                    "BAD_REQUEST", "Bad Request", e.getMessage());
            case jakarta.ws.rs.NotFoundException e -> buildResponse(Response.Status.NOT_FOUND,
                    "NOT_FOUND", "Not Found", e.getMessage());
            default -> buildResponse(Response.Status.INTERNAL_SERVER_ERROR,
                    "INTERNAL_ERROR", "Internal Server Error", "An unexpected error occurred");
        };
    }

    private Response buildResponse(Response.Status status, String code, String title, String detail) {
        ErrorResponse error = ErrorResponse.of(code, detail, status.getStatusCode(),
                URI.create("urn:wallet:notification:" + code), null);
        return Response.status(status).entity(error).build();
    }
}
