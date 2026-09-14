package com.wallet.shared.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Single ObjectMapper configuration shared by every service.
 * Why central: identical JSON shape across services (and across Kafka payloads)
 * is critical to avoid silent breakage when one service rolls out ahead of another.
 *
 * Choices:
 * - ISO-8601 dates (not numeric timestamps): readable in logs and Kafka payloads.
 * - java.time.Instant/LocalDateTime work out of the box (JavaTimeModule).
 * - FAIL_ON_UNKNOWN_PROPERTIES=false: forward-compatible consumers.
 * - camelCase: simplest convention; consumers in the same repo can rely on it.
 */
public final class JsonUtil {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .serializationInclusion(JsonInclude.Include.NON_NULL)
        .build();

    private JsonUtil() { }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("failed to serialize to JSON", e);
        }
    }

    public static <T> T fromJson(String raw, Class<T> type) {
        try {
            return MAPPER.readValue(raw, type);
        } catch (Exception e) {
            throw new IllegalStateException("failed to deserialize JSON", e);
        }
    }
}
