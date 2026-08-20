package com.wallet.payment.infrastructure.idempotency;

import com.wallet.payment.infrastructure.idempotency.IdempotencyService.IdempotencyResult;
import com.wallet.shared.util.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    @SuppressWarnings("rawtypes")
    private ValueOperations valueOps;

    @InjectMocks
    private IdempotencyService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void wireValueOps() {
        given(redis.opsForValue()).willReturn(valueOps);
        ReflectionTestUtils.setField(service, "ttlHours", 24L);
    }

    @Nested
    @DisplayName("Slot vacío + claim exitoso")
    class SlotVacioClaimExitoso {

        @Test
        @DisplayName("Ejecuta la acción, cachea el resultado y retorna executed")
        @SuppressWarnings("unchecked")
        void ejecutaAccion_cachea_retornaExecuted() {
            String key = "k-1";
            given(valueOps.get("wallet:idem:" + key)).willReturn(null);
            given(valueOps.setIfAbsent(eq("wallet:idem:" + key), eq("PENDING"), any(Duration.class)))
                .willReturn(true);

            IdempotencyResult<String> result = service.executeOnce(key, String.class, () -> "result-1");

            assertThat(result.replayed()).isFalse();
            assertThat(result.value()).isEqualTo("result-1");

            verify(valueOps).set(eq("wallet:idem:" + key), eq(JsonUtil.toJson("result-1")), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("Slot vacío + claim falla (race)")
    class SlotVacioClaimFalla {

        @Test
        @DisplayName("Lee el resultado cacheado por el ganador y retorna replayed")
        @SuppressWarnings("unchecked")
        void reFetchDevuelveCached_retornaReplayed() {
            String key = "k-1";
            String cachedJson = JsonUtil.toJson("result-from-other-thread");

            given(valueOps.get("wallet:idem:" + key))
                .willReturn(null)
                .willReturn(cachedJson);
            given(valueOps.setIfAbsent(eq("wallet:idem:" + key), eq("PENDING"), any(Duration.class)))
                .willReturn(false);

            AtomicInteger callCount = new AtomicInteger();
            IdempotencyResult<String> result = service.executeOnce(key, String.class, () -> {
                callCount.incrementAndGet();
                return "should-not-run";
            });

            assertThat(result.replayed()).isTrue();
            assertThat(result.value()).isEqualTo("result-from-other-thread");
            assertThat(callCount.get())
                .as("la acción nunca debe ejecutarse cuando el claim falla")
                .isZero();
        }
    }

    @Nested
    @DisplayName("Slot ya lleno")
    class SlotYaLleno {

        @Test
        @DisplayName("Retorna replayed sin intentar claim ni ejecutar la acción")
        @SuppressWarnings("unchecked")
        void retornaReplayed_sinClaim_sinEjecucion() {
            String key = "k-1";
            String cachedJson = JsonUtil.toJson("result-cached");

            given(valueOps.get("wallet:idem:" + key)).willReturn(cachedJson);

            AtomicInteger callCount = new AtomicInteger();
            IdempotencyResult<String> result = service.executeOnce(key, String.class, () -> {
                callCount.incrementAndGet();
                return "should-not-run";
            });

            assertThat(result.replayed()).isTrue();
            assertThat(result.value()).isEqualTo("result-cached");
            assertThat(callCount.get()).isZero();
            verify(valueOps, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("La acción lanza excepción")
    class AccionLanzaExcepcion {

        @Test
        @DisplayName("Libera el slot y re-lanza la excepción")
        @SuppressWarnings("unchecked")
        void liberaSlot_y_relanzaExcepcion() {
            String key = "k-1";
            given(valueOps.get("wallet:idem:" + key)).willReturn(null);
            given(valueOps.setIfAbsent(eq("wallet:idem:" + key), eq("PENDING"), any(Duration.class)))
                .willReturn(true);

            RuntimeException original = new RuntimeException("boom");

            assertThatThrownBy(() -> service.executeOnce(key, String.class, () -> { throw original; }))
                .isSameAs(original);

            verify(redis).delete("wallet:idem:" + key);
            verify(valueOps, never())
                .set(eq("wallet:idem:" + key), anyString(), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("Tipos complejos")
    class TiposComplejos {

        @Test
        @DisplayName("Deserializa correctamente el tipo genérico pasado como Class<T>")
        @SuppressWarnings("unchecked")
        void deserializaTipoGenerico() {
            String key = "k-pojo";
            Pojo value = new Pojo("id-1", 42);
            String cached = JsonUtil.toJson(value);

            given(valueOps.get("wallet:idem:" + key)).willReturn(cached);

            IdempotencyResult<Pojo> result = service.executeOnce(key, Pojo.class, () -> {
                throw new AssertionError("acción no debe ejecutarse");
            });

            assertThat(result.replayed()).isTrue();
            assertThat(result.value()).isEqualTo(value);
            assertThat(result.value().id()).isEqualTo("id-1");
            assertThat(result.value().count()).isEqualTo(42);
        }

        @Test
        @DisplayName("Executed devuelve el valor exacto retornado por la acción")
        @SuppressWarnings("unchecked")
        void executed_retornaValorExacto() {
            String key = "k-pojo";
            given(valueOps.get("wallet:idem:" + key)).willReturn(null);
            given(valueOps.setIfAbsent(eq("wallet:idem:" + key), eq("PENDING"), any(Duration.class)))
                .willReturn(true);

            Pojo produced = new Pojo("id-2", 7);
            IdempotencyResult<Pojo> result = service.executeOnce(key, Pojo.class, () -> produced);

            assertThat(result.replayed()).isFalse();
            assertThat(result.value()).isSameAs(produced);
            verify(valueOps, times(1))
                .set(eq("wallet:idem:" + key), eq(JsonUtil.toJson(produced)), any(Duration.class));
        }
    }

    public record Pojo(String id, int count) { }
}
