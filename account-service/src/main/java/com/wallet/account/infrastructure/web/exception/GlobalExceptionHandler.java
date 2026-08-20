package com.wallet.account.infrastructure.web.exception;

import com.wallet.account.domain.exception.AccountNotFoundException;
import com.wallet.account.domain.exception.ConcurrentModificationException;
import com.wallet.account.domain.exception.InsufficientFundsException;
import com.wallet.shared.api.ErrorResponse;
import com.wallet.shared.context.CorrelationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(AccountNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> insufficient(InsufficientFundsException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_FUNDS", ex.getMessage());
    }

    @ExceptionHandler(ConcurrentModificationException.class)
    public ResponseEntity<ErrorResponse> concurrent(ConcurrentModificationException ex) {
        return build(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .findFirst()
            .orElse("validation failed");
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED", detail);
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
        return ResponseEntity.status(status).body(body);
    }
}
