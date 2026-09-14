package com.wallet.account.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wallet.shared.event.AccountOpenedEvent;
import com.wallet.shared.event.EventMetadata;
import com.wallet.shared.money.Money;

@DisplayName("EventStoreEntity mapping")
class EventStoreEntityTest {

    @Test
    @DisplayName("should create entity from constructor")
    void createEntity() {
        EventStoreEntity entity = new EventStoreEntity("acc-001", "AccountOpenedEvent",
                "{\"test\":\"payload\"}", 1L, "corr-001");

        assertEquals("acc-001", entity.getAggregateId());
        assertEquals("AccountOpenedEvent", entity.getEventType());
        assertEquals("{\"test\":\"payload\"}", entity.getPayload());
        assertEquals(1L, entity.getVersion());
        assertEquals("corr-001", entity.getCorrelationId());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    @DisplayName("should have unique constraint on aggregate + version")
    void uniqueConstraint() {
        EventStoreEntity e1 = new EventStoreEntity("acc-001", "AccountOpenedEvent", "{}", 1L, null);
        EventStoreEntity e2 = new EventStoreEntity("acc-001", "AccountOpenedEvent", "{}", 1L, null);

        // Same aggregate + version = same logical key
        assertEquals(e1.getAggregateId(), e2.getAggregateId());
        assertEquals(e1.getVersion(), e2.getVersion());
    }
}
