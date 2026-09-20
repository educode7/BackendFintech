package com.wallet.account.infrastructure.exception;

import java.net.URI;
import java.time.Instant;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.wallet.account.domain.exception.AccountNotFoundException;
import com.wallet.account.domain.exception.ConcurrentModificationException;
import com.wallet.account.domain.exception.InsufficientFundsException;
import com.wallet.shared.api.ErrorResponse;

/**
 * Global exception mapper for Account Service.
 */
@Provider
public class AccountExceptionMapper implements ExceptionMapper<RuntimeException> {

    private static final Logger log = Logger.getLogger(AccountExceptionMapper.class);

    @Override
    public Response toResponse(RuntimeException exception) {
        log.errorf(exception, "Handling unhandled exception: %s", exception.getClass().getName());
        return switch (exception) {
            case AccountNotFoundException e -> buildResponse(
                    Response.Status.NOT_FOUND,
                    "urn:wallet:ACCOUNT_NOT_FOUND", "Account Not Found",
                    e.getMessage(), "ACCOUNT_NOT_FOUND");

            case ConcurrentModificationException e -> buildResponse(
                    Response.Status.CONFLICT,
                    "urn:wallet:CONCURRENT_MODIFICATION", "Concurrent Modification",
                    e.getMessage(), "CONCURRENT_MODIFICATION");

            case InsufficientFundsException e -> buildResponse(
                    Response.Status.CONFLICT,
                    "urn:wallet:INSUFFICIENT_FUNDS", "Insufficient Funds",
                    e.getMessage(), "INSUFFICIENT_FUNDS");

            case jakarta.validation.ConstraintViolationException e -> buildResponse(
                    422,
                    "urn:wallet:VALIDATION_ERROR", "Validation Error",
                    e.getMessage(), "VALIDATION_ERROR");

            case IllegalArgumentException e -> buildResponse(
                    Response.Status.BAD_REQUEST,
                    "urn:wallet:BAD_REQUEST", "Bad Request",
                    e.getMessage(), "BAD_REQUEST");

            default -> buildResponse(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "urn:wallet:INTERNAL_ERROR", "Internal Server Error",
                    "An unexpected error occurred", "INTERNAL_ERROR");
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
