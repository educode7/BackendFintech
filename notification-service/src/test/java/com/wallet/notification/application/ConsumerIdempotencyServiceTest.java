package com.wallet.notification.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ConsumerIdempotencyServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    @SuppressWarnings("rawtypes")
    private ValueOperations valueOps;

    @org.mockito.InjectMocks
    private ConsumerIdempotencyService service;

    @org.junit.jupiter.api.BeforeEach
    @SuppressWarnings("unchecked")
    void wireValueOps() {
        given(redis.opsForValue()).willReturn(valueOps);
        ReflectionTestUtils.setField(service, "ttlDays", 7L);
    }

    @Test
    @DisplayName("tryClaim: SET NX retorna true → devuelve true (claim exitoso)")
    @SuppressWarnings("unchecked")
    void tryClaim_true() {
        given(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).willReturn(true);

        boolean claimed = service.tryClaim("evt-1");

        assertThat(claimed).isTrue();
    }

    @Test
    @DisplayName("tryClaim: SET NX retorna false → devuelve false (claim falla)")
    @SuppressWarnings("unchecked")
    void tryClaim_false() {
        given(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).willReturn(false);

        boolean claimed = service.tryClaim("evt-2");

        assertThat(claimed).isFalse();
    }

    @Test
    @DisplayName("tryClaim: SET NX retorna null (no es Boolean.TRUE) → devuelve false")
    @SuppressWarnings("unchecked")
    void tryClaim_null() {
        given(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).willReturn(null);

        boolean claimed = service.tryClaim("evt-3");

        assertThat(claimed).isFalse();
    }

    @Test
    @DisplayName("tryClaim: el key lleva el prefijo 'notification:processed:'")
    @SuppressWarnings("unchecked")
    void tryClaim_prefijo() {
        given(valueOps.setIfAbsent(eq("notification:processed:evt-x"), anyString(), any(Duration.class)))
            .willReturn(true);

        boolean claimed = service.tryClaim("evt-x");

        assertThat(claimed).isTrue();
    }
}
