package com.wallet.shared.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Single ObjectMapper configuration shared by every service.
 * Why central: identical JSON shape across services (and across Kafka payloads)
 * is critical to avoid silent breakage when one service rolls out ahead of another.
 *
 * Choices:
 * - ISO-8601 dates (not numeric timestamps): readable in logs and Kafka payloads.
 * - java.time.Instant/LocalDateTime work out of the box (Jackson 3 built-in).
 * - FAIL_ON_UNKNOWN_PROPERTIES=false: forward-compatible consumers.
 * - camelCase: simplest convention; consumers in the same repo can rely on it.
 */
public final class JsonUtil {

    private static final JsonMapper MAPPER = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .changeDefaultPropertyInclusion(prev -> prev.withOverrides(
            JsonInclude.Value.construct(JsonInclude.Include.USE_DEFAULTS, JsonInclude.Include.NON_NULL)))
        .build();

    private JsonUtil() { }

    public static JsonMapper mapper() {
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
