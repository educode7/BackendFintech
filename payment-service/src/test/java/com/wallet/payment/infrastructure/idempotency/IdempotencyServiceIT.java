package com.wallet.payment.infrastructure.idempotency;

import com.wallet.payment.infrastructure.idempotency.IdempotencyService.IdempotencyResult;
import com.wallet.payment.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyServiceIT extends AbstractIntegrationTest {

    @Autowired
    private IdempotencyService idempotency;

    @Test
    @DisplayName("Dos ejecuciones con misma key: la segunda retorna replayed sin ejecutar la acción")
    void ejecucion_unica_cacheaResultado() {
        String key = "idem-it-cache-" + System.nanoTime();
        AtomicInteger callCount = new AtomicInteger();

        IdempotencyResult<String> first = idempotency.executeOnce(key, String.class, () -> {
            callCount.incrementAndGet();
            return "value";
        });
        IdempotencyResult<String> second = idempotency.executeOnce(key, String.class, () -> {
            callCount.incrementAndGet();
            return "value";
        });

        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(second.value()).isEqualTo("value");
        assertThat(callCount.get())
            .as("la acción solo debe ejecutarse una vez")
            .isEqualTo(1);
    }

    @Test
    @DisplayName("Distinta key crea slots separados")
    void ejecucion_diferenteKey_creaSlotsSeparados() {
        String keyA = "idem-it-a-" + System.nanoTime();
        String keyB = "idem-it-b-" + System.nanoTime();

        IdempotencyResult<String> a = idempotency.executeOnce(keyA, String.class, () -> "valueA");
        IdempotencyResult<String> b = idempotency.executeOnce(keyB, String.class, () -> "valueB");

        assertThat(a.replayed()).isFalse();
        assertThat(b.replayed()).isFalse();
        assertThat(a.value()).isEqualTo("valueA");
        assertThat(b.value()).isEqualTo("valueB");
    }

    @Test
    @DisplayName("Si la acción falla, el slot se libera y el próximo intento ejecuta de nuevo")
    void ejecucion_conFallo_liberaSlot() {
        String key = "idem-it-fail-" + System.nanoTime();

        assertThatThrownBy(() -> idempotency.executeOnce(key, String.class, () -> {
            throw new RuntimeException("boom");
        })).isInstanceOf(RuntimeException.class).hasMessage("boom");

        AtomicInteger callCount = new AtomicInteger();
        IdempotencyResult<String> recovered = idempotency.executeOnce(key, String.class, () -> {
            callCount.incrementAndGet();
            return "value-after-fail";
        });

        assertThat(recovered.replayed()).isFalse();
        assertThat(recovered.value()).isEqualTo("value-after-fail");
        assertThat(callCount.get())
            .as("la acción debe re-ejecutarse tras la liberación del slot")
            .isEqualTo(1);
    }
}
