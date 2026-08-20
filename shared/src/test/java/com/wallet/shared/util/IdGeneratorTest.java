package com.wallet.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IdGeneratorTest {

    @Test
    @DisplayName("Genera IDs de 36 caracteres en formato UUID")
    void genera_idsDe36Caracteres() {
        String id = IdGenerator.newId();

        assertThat(id).hasSize(36);
        assertThat(id.charAt(8)).isEqualTo('-');
        assertThat(id.charAt(13)).isEqualTo('-');
        assertThat(id.charAt(18)).isEqualTo('-');
        assertThat(id.charAt(23)).isEqualTo('-');
    }

    @Test
    @DisplayName("El ID parsea como UUID válido")
    void parsea_comoUuidValido() {
        String id = IdGenerator.newId();

        UUID parsed = UUID.fromString(id);

        assertThat(parsed).isNotNull();
        assertThat(parsed.toString()).isEqualTo(id);
    }

    @Test
    @DisplayName("Es UUID v7: la versión está en los 4 bits altos del 7º byte")
    void esUuidVersion7() {
        UUID uuid = UUID.fromString(IdGenerator.newId());

        int version = uuid.version();
        assertThat(version).isEqualTo(7);
    }

    @Test
    @DisplayName("UUIDs v7 son time-ordered: IDs consecutivos son comparables")
    void uuidsConsecutivos_sonOrdenables() throws InterruptedException {
        String first = IdGenerator.newId();

        Thread.sleep(2);

        String second = IdGenerator.newId();

        UUID firstUuid = UUID.fromString(first);
        UUID secondUuid = UUID.fromString(second);

        assertThat(secondUuid.getMostSignificantBits())
            .isGreaterThan(firstUuid.getMostSignificantBits());
    }

    @Test
    @DisplayName("Los primeros 48 bits codifican el timestamp en milisegundos")
    void primeros48Bits_sonTimestamp() {
        Instant before = Instant.now();

        String id = IdGenerator.newId();
        UUID uuid = UUID.fromString(id);

        Instant after = Instant.now();

        long timestampMs = uuid.getMostSignificantBits() >>> 16;

        assertThat(timestampMs).isBetween(before.toEpochMilli(), after.toEpochMilli());
    }

    @Test
    @DisplayName("IDs generados rápidamente son únicos")
    void ids_generadosRapidamente_sonUnicos() {
        int sampleSize = 1000;
        java.util.Set<String> ids = new java.util.HashSet<>(sampleSize * 2);

        for (int i = 0; i < sampleSize; i++) {
            assertThat(ids.add(IdGenerator.newId()))
                .as("ID duplicado tras %d iteraciones", i)
                .isTrue();
        }
    }
}
