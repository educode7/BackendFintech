package com.wallet.payment.infrastructure.exception;

import java.net.URI;
import java.time.Instant;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.wallet.payment.domain.exception.DuplicatePaymentException;
import com.wallet.payment.domain.exception.MissingIdempotencyKeyException;
import com.wallet.payment.domain.exception.PaymentNotFoundException;
import com.wallet.shared.api.ErrorResponse;

/**
 * Global exception mapper — translates domain exceptions to RFC 9457 Problem+JSON.
 */
@Provider
public class PaymentExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException exception) {
        return switch (exception) {
            case PaymentNotFoundException e -> buildResponse(
                    Response.Status.NOT_FOUND,
                    "urn:wallet:PAYMENT_NOT_FOUND",
                    "Payment Not Found",
                    e.getMessage(),
                    "PAYMENT_NOT_FOUND");

            case MissingIdempotencyKeyException e -> buildResponse(
                    Response.Status.BAD_REQUEST,
                    "urn:wallet:IDEMPOTENCY_KEY_MISSING",
                    "Idempotency Key Missing",
                    e.getMessage(),
                    "IDEMPOTENCY_KEY_MISSING");

            case DuplicatePaymentException e -> buildResponse(
                    Response.Status.CONFLICT,
                    "urn:wallet:DUPLICATE_PAYMENT",
                    "Duplicate Payment",
                    e.getMessage(),
                    "DUPLICATE_PAYMENT");

            case jakarta.validation.ConstraintViolationException e -> buildResponse(
                    422,
                    "urn:wallet:VALIDATION_ERROR",
                    "Validation Error",
                    e.getMessage(),
                    "VALIDATION_ERROR");

            case IllegalArgumentException e -> buildResponse(
                    Response.Status.BAD_REQUEST,
                    "urn:wallet:BAD_REQUEST",
                    "Bad Request",
                    e.getMessage(),
                    "BAD_REQUEST");

            default -> buildResponse(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "urn:wallet:INTERNAL_ERROR",
                    "Internal Server Error",
                    "An unexpected error occurred",
                    "INTERNAL_ERROR");
        };
    }

    private Response buildResponse(int statusCode, String type, String title,
                                   String detail, String code) {
        ErrorResponse error = ErrorResponse.of(code, detail, statusCode,
                URI.create(type), null);
        return Response.status(statusCode).entity(error).build();
    }

    private Response buildResponse(Response.Status status, String type, String title,
                                   String detail, String code) {
        return buildResponse(status.getStatusCode(), type, title, detail, code);
    }
}
