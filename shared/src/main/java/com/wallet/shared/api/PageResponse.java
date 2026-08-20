package com.wallet.shared.api;

import java.util.List;

/**
 * Generic paginated response envelope.
 * Keeping it tiny on purpose — clients only need items, total, page, size.
 * Why a custom envelope instead of Spring's PageImpl: PageImpl serializes as JSON
 * with Spring-specific fields that don't round-trip well across services.
 */
public record PageResponse<T>(
    List<T> items,
    long total,
    int page,
    int size
) {
    public static <T> PageResponse<T> of(List<T> items, long total, int page, int size) {
        return new PageResponse<>(items, total, page, size);
    }
}
