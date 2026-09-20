package com.wallet.payment.infrastructure.web;

import java.net.URI;
import java.time.Instant;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.wallet.shared.api.ErrorResponse;

/**
 * Global exception mapper that logs ALL unhandled exceptions with full stack traces.
 * This is critical for debugging 500 errors in Docker where the default Quarkus
 * exception mapper may not log the full stack trace.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger log = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        // Log the FULL stack trace - this is the key difference from default behavior
        log.errorf(exception, "Unhandled exception: %s - %s",
                exception.getClass().getSimpleName(),
                exception.getMessage());

        // Build error response
        String errorType = "INTERNAL_ERROR";
        int status = 500;

        if (exception instanceof jakarta.validation.ConstraintViolationException) {
            errorType = "VALIDATION_ERROR";
            status = 422;
        } else if (exception instanceof com.wallet.payment.domain.exception.DuplicatePaymentException) {
            errorType = "DUPLICATE_PAYMENT";
            status = 409;
        } else if (exception instanceof com.wallet.payment.domain.exception.MissingIdempotencyKeyException) {
            errorType = "IDEMPOTENCY_KEY_MISSING";
            status = 400;
        }

        ErrorResponse errorResponse = new ErrorResponse(
                URI.create("urn:wallet:payment:" + errorType),
                errorType.replace("_", " "),
                status,
                exception.getMessage(),
                null,
                errorType,
                null,
                Instant.now()
        );

        return Response.status(status).entity(errorResponse).build();
    }
}
