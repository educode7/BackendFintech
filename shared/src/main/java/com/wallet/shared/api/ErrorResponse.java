package com.wallet.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.net.URI;
import java.time.Instant;

/**
 * RFC 9457 Problem+JSON extended with stable business error codes and traceability.
 * Why extend ProblemDetail: Spring already speaks RFC 9457 (application/problem+json),
 * we just need our own contract on top so clients can switch on `code` reliably.
 *
 * - `type` is a URI identifying the error category (often a docs URL).
 * - `code` is a stable, machine-readable error code (e.g. "PAYMENT_NOT_FOUND").
 * - `correlationId` ties the response to the log line and the upstream trace.
 *
 * Kept framework-agnostic on purpose: the `shared` module does not depend on Spring
 * so each service maps this to/from Spring's {@code ProblemDetail} at the boundary.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    URI type,
    String title,
    int status,
    String detail,
    URI instance,
    String code,
    String correlationId,
    Instant timestamp
) {

    public static ErrorResponse of(String code, String detail, int status, URI type, String correlationId) {
        return new ErrorResponse(
            type,
            code,
            status,
            detail,
            null,
            code,
            correlationId,
            Instant.now()
        );
    }
}
