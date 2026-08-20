package com.wallet.shared.api;

import com.wallet.shared.util.JsonUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseJsonContractTest {

    @Test
    @DisplayName("round-trip preserva type, status, detail, code y correlationId")
    void roundTrip() {
        Instant ts = Instant.parse("2026-01-15T10:00:00Z");
        ErrorResponse original = new ErrorResponse(
            URI.create("https://errors.example.com/code"),
            "CODE",
            409,
            "conflict detail",
            URI.create("/api/v1/payments"),
            "CODE",
            "corr-1",
            ts
        );

        String json = JsonUtil.toJson(original);
        ErrorResponse parsed = JsonUtil.fromJson(json, ErrorResponse.class);

        assertThat(parsed.type()).isEqualTo(original.type());
        assertThat(parsed.title()).isEqualTo(original.title());
        assertThat(parsed.status()).isEqualTo(original.status());
        assertThat(parsed.detail()).isEqualTo(original.detail());
        assertThat(parsed.code()).isEqualTo(original.code());
        assertThat(parsed.correlationId()).isEqualTo(original.correlationId());
        assertThat(parsed.timestamp()).isEqualTo(original.timestamp());
    }

    @Test
    @DisplayName("JSON incluye los 8 campos esperados")
    void jsonContieneCamposEsperados() {
        ErrorResponse response = ErrorResponse.of(
            "DUPLICATE_PAYMENT",
            "idempotency key already used",
            409,
            URI.create("about:blank"),
            "corr-9"
        );

        String json = JsonUtil.toJson(response);

        assertThat(json).contains("\"type\":\"about:blank\"");
        assertThat(json).contains("\"title\":\"DUPLICATE_PAYMENT\"");
        assertThat(json).contains("\"status\":409");
        assertThat(json).contains("\"detail\":\"idempotency key already used\"");
        assertThat(json).contains("\"code\":\"DUPLICATE_PAYMENT\"");
        assertThat(json).contains("\"correlationId\":\"corr-9\"");
        assertThat(json).contains("\"timestamp\"");
    }

    @Test
    @DisplayName("JSON omite instance cuando es null (NON_NULL)")
    void jsonOmiteInstanceNull() {
        ErrorResponse response = ErrorResponse.of("C", "d", 400, URI.create("about:blank"), null);

        String json = JsonUtil.toJson(response);

        assertThat(json).doesNotContain("\"instance\"");
    }
}
