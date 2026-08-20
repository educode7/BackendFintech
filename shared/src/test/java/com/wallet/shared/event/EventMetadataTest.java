package com.wallet.shared.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventMetadataTest {

    @Test
    @DisplayName("create rellena eventId, occurredAt, correlationId y version")
    void create_rellenaTodosLosCampos() {
        Instant before = Instant.now();
        EventMetadata metadata = EventMetadata.create("corr-123", 5);
        Instant after = Instant.now();

        assertThat(metadata.eventId()).isNotBlank();
        assertThat(UUID.fromString(metadata.eventId())).isNotNull();
        assertThat(metadata.occurredAt()).isBetween(before, after);
        assertThat(metadata.correlationId()).isEqualTo("corr-123");
        assertThat(metadata.version()).isEqualTo(5);
    }

    @Test
    @DisplayName("create acepta correlationId null")
    void create_aceptaCorrelationIdNull() {
        EventMetadata metadata = EventMetadata.create(null, 1);

        assertThat(metadata.correlationId()).isNull();
        assertThat(metadata.eventId()).isNotBlank();
        assertThat(metadata.version()).isEqualTo(1);
    }

    @Test
    @DisplayName("Dos metadatas con mismos valores son iguales")
    void equals_porValor() {
        Instant now = Instant.now();
        EventMetadata a = new EventMetadata("evt-1", now, "corr-1", 1);
        EventMetadata b = new EventMetadata("evt-1", now, "corr-1", 1);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("Metadatas con distinto eventId no son iguales")
    void equals_distintoEventId_false() {
        Instant now = Instant.now();
        EventMetadata a = new EventMetadata("evt-1", now, "corr-1", 1);
        EventMetadata b = new EventMetadata("evt-2", now, "corr-1", 1);

        assertThat(a).isNotEqualTo(b);
    }
}
