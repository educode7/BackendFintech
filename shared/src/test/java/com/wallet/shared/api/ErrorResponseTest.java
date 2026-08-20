package com.wallet.shared.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    @DisplayName("of rellena timestamp en el momento de la llamada")
    void of_rellenaTimestampReciente() {
        InstantRange range = InstantRange.now();

        ErrorResponse response = ErrorResponse.of("CODE", "detail", 404, URI.create("about:blank"), "corr-1");

        assertThat(response.timestamp()).isBetween(range.before(), range.after());
    }

    @Test
    @DisplayName("of preserva type, status, detail, correlationId y code")
    void of_preservaCampos() {
        ErrorResponse response = ErrorResponse.of(
            "PAYMENT_NOT_FOUND",
            "payment not found: abc",
            404,
            URI.create("https://errors.example.com/payment-not-found"),
            "corr-abc"
        );

        assertThat(response.code()).isEqualTo("PAYMENT_NOT_FOUND");
        assertThat(response.title()).isEqualTo("PAYMENT_NOT_FOUND");
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.detail()).isEqualTo("payment not found: abc");
        assertThat(response.type()).isEqualTo(URI.create("https://errors.example.com/payment-not-found"));
        assertThat(response.correlationId()).isEqualTo("corr-abc");
    }

    @Test
    @DisplayName("of pone instance en null (se rellena en la capa web)")
    void of_instanceEsNull() {
        ErrorResponse response = ErrorResponse.of("CODE", "d", 500, URI.create("about:blank"), null);

        assertThat(response.instance()).isNull();
    }

    @Test
    @DisplayName("equals es por valor")
    void equals_porValor() {
        java.time.Instant ts = java.time.Instant.parse("2026-01-01T00:00:00Z");
        ErrorResponse a = new ErrorResponse(
            URI.create("about:blank"), "C", 400, "d", null, "C", "x", ts
        );
        ErrorResponse b = new ErrorResponse(
            URI.create("about:blank"), "C", 400, "d", null, "C", "x", ts
        );

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    /**
     * Captura del rango temporal en el que se ejecuta el SUT.
     */
    private record InstantRange(java.time.Instant before, java.time.Instant after) {
        static InstantRange now() {
            java.time.Instant before = java.time.Instant.now();
            try { Thread.sleep(Duration.ofMillis(1).toMillis()); } catch (InterruptedException ignored) { }
            java.time.Instant after = java.time.Instant.now();
            return new InstantRange(before, after);
        }
    }
}
