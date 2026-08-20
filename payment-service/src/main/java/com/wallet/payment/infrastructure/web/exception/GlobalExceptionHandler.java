package com.wallet.payment.infrastructure.web.exception;

import com.wallet.shared.api.ErrorResponse;
import com.wallet.shared.context.CorrelationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.net.URI;

/**
 * Centralised exception mapping → RFC 9457 ProblemDetail.
 * Every error response carries:
 *  - stable business code (so clients can switch on it)
 *  - correlationId (for log lookup)
 *  - timestamp (so cached error pages stay honest)
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(PaymentNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(MissingIdempotencyKeyException.class)
    public ResponseEntity<ErrorResponse> missingKey(MissingIdempotencyKeyException ex) {
        return build(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED", ex.getMessage());
    }

    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<ErrorResponse> duplicate(DuplicatePaymentException ex) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_PAYMENT", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException ex) {
        // Most commonly: UNIQUE violation on idempotency_key.
        return build(HttpStatus.CONFLICT, "DUPLICATE_PAYMENT", "idempotency key already used");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .findFirst()
            .orElse("validation failed");
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED", detail);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> notMapped(NoHandlerFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> illegal(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> fallback(Exception ex) {
        log.error("unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "unexpected error");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String detail) {
        String correlationId = CorrelationContext.currentOrNull();
        ErrorResponse body = ErrorResponse.of(code, detail, status.value(), URI.create("about:blank"), correlationId);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(code);
        pd.setType(URI.create("about:blank"));
        pd.setProperty("code", code);
        pd.setProperty("correlationId", correlationId);
        // Returning our richer envelope so clients can read the stable fields directly.
        return ResponseEntity.status(status).body(body);
    }
}
