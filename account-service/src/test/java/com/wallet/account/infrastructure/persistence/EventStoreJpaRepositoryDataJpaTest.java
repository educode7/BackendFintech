package com.wallet.account.infrastructure.persistence;

import com.wallet.shared.event.AccountOpenedEvent;
import com.wallet.shared.event.EventMetadata;
import com.wallet.shared.event.MoneyDepositedEvent;
import com.wallet.shared.event.MoneyWithdrawnEvent;
import com.wallet.shared.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Slice JPA test para {@link EventStoreJpaRepository}.
 *
 * <p>Spring Boot 4 moved {@code @DataJpaTest} and {@code TestEntityManager} to
 * the {@code org.springframework.boot.data.jpa.test.autoconfigure} and
 * {@code org.springframework.boot.jpa.test.autoconfigure} packages respectively.
 * {@code @AutoConfigureTestDatabase} is no longer needed in 4.x.
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:accountdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.show-sql=true",
    "spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=classpath:schema-h2.sql",
    "spring.flyway.enabled=false"
})
class EventStoreJpaRepositoryDataJpaTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private EventStoreJpaRepository repo;

    @Test
    @DisplayName("save persiste el row del event store")
    void save_persiste() {
        EventStoreEntity row = sample("acc-1", 1L, "AccountOpenedEvent");
        EventStoreEntity saved = em.persistAndFlush(row);

        em.clear();
        EventStoreEntity found = repo.findById(saved.getId()).orElseThrow();

        assertThat(found.getAggregateId()).isEqualTo("acc-1");
        assertThat(found.getVersion()).isEqualTo(1L);
        assertThat(found.getEventType()).isEqualTo("AccountOpenedEvent");
    }

    @Test
    @DisplayName("findByAggregateIdOrderByVersionAsc retorna eventos ordenados")
    void findByAggregateId_retornaOrdenados() {
        em.persistAndFlush(sample("acc-1", 1L, "AccountOpenedEvent"));
        em.persistAndFlush(sample("acc-1", 3L, "MoneyWithdrawnEvent"));
        em.persistAndFlush(sample("acc-1", 2L, "MoneyDepositedEvent"));
        em.persistAndFlush(sample("acc-2", 1L, "AccountOpenedEvent"));
        em.clear();

        List<EventStoreEntity> rows = repo.findByAggregateIdOrderByVersionAsc("acc-1");

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).getVersion()).isEqualTo(1L);
        assertThat(rows.get(1).getVersion()).isEqualTo(2L);
        assertThat(rows.get(2).getVersion()).isEqualTo(3L);
        assertThat(rows.get(0).getEventType()).isEqualTo("AccountOpenedEvent");
        assertThat(rows.get(1).getEventType()).isEqualTo("MoneyDepositedEvent");
        assertThat(rows.get(2).getEventType()).isEqualTo("MoneyWithdrawnEvent");
    }

    @Test
    @DisplayName("deleteById elimina el row del event store")
    void deleteById_elimina() {
        EventStoreEntity saved = em.persistAndFlush(sample("acc-del", 1L, "AccountOpenedEvent"));

        repo.deleteById(saved.getId());
        em.flush();

        assertThat(repo.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("Round-trip JSON: el eventData serializado se puede deserializar al evento original")
    void roundTrip_json() {
        AccountOpenedEvent original = new AccountOpenedEvent(
            "acc-1", "user-1",
            new Money(new BigDecimal("100.00"), "USD"),
            new EventMetadata("evt-1", Instant.parse("2026-01-15T10:00:00Z"), "corr-1", 1)
        );

        EventStoreEntity row = new EventStoreEntity();
        row.setId("evt-row-1");
        row.setAggregateId("acc-1");
        row.setAggregateType("Account");
        row.setEventType("AccountOpenedEvent");
        row.setEventData(com.wallet.shared.util.JsonUtil.toJson(original));
        row.setVersion(1L);
        row.setCreatedAt(Instant.now());
        em.persistAndFlush(row);
        em.clear();

        EventStoreEntity found = repo.findById("evt-row-1").orElseThrow();
        AccountOpenedEvent parsed = com.wallet.shared.util.JsonUtil.fromJson(found.getEventData(), AccountOpenedEvent.class);

        assertThat(parsed).isEqualTo(original);
    }

    @Test
    @DisplayName("Round-trip JSON: MoneyDepositedEvent")
    void roundTrip_deposited() {
        MoneyDepositedEvent original = new MoneyDepositedEvent(
            "acc-1",
            new Money(new BigDecimal("50.00"), "USD"),
            new Money(new BigDecimal("150.00"), "USD"),
            new EventMetadata("evt-2", Instant.now(), "corr-2", 2)
        );

        EventStoreEntity row = new EventStoreEntity();
        row.setId("evt-row-2");
        row.setAggregateId("acc-1");
        row.setAggregateType("Account");
        row.setEventType("MoneyDepositedEvent");
        row.setEventData(com.wallet.shared.util.JsonUtil.toJson(original));
        row.setVersion(2L);
        row.setCreatedAt(Instant.now());
        em.persistAndFlush(row);
        em.clear();

        MoneyDepositedEvent parsed = com.wallet.shared.util.JsonUtil.fromJson(
            repo.findById("evt-row-2").orElseThrow().getEventData(),
            MoneyDepositedEvent.class);

        assertThat(parsed).isEqualTo(original);
    }

    private static EventStoreEntity sample(String aggregateId, long version, String eventType) {
        EventStoreEntity row = new EventStoreEntity();
        row.setId("evt-" + aggregateId + "-" + version);
        row.setAggregateId(aggregateId);
        row.setAggregateType("Account");
        row.setEventType(eventType);
        row.setEventData("{}");
        row.setVersion(version);
        row.setCreatedAt(Instant.now());
        row.setCorrelationId("corr-test");
        return row;
    }
}
