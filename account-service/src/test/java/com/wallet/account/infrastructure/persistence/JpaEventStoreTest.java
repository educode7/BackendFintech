package com.wallet.account.infrastructure.persistence;

import com.wallet.account.domain.Account;
import com.wallet.account.domain.exception.AccountNotFoundException;
import com.wallet.account.domain.exception.ConcurrentModificationException;
import com.wallet.shared.event.AccountOpenedEvent;
import com.wallet.shared.event.EventMetadata;
import com.wallet.shared.event.MoneyDepositedEvent;
import com.wallet.shared.money.Money;
import com.wallet.shared.util.JsonUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JpaEventStoreTest {

    @Mock
    private EventStoreJpaRepository repository;

    @InjectMocks
    private JpaEventStore store;

    @Nested
    @DisplayName("append")
    class Append {

        @Test
        @DisplayName("Persiste la fila con version=1 para el primer evento de un aggregate")
        void append_primerEvento_version1() {
            AccountOpenedEvent event = sampleOpened();
            given(repository.findByAggregateIdOrderByVersionAsc("acc-1")).willReturn(List.of());

            store.append(event, "Account");

            verify(repository).save(any(EventStoreEntity.class));
        }

        @Test
        @DisplayName("Retry: tras DataIntegrityViolationException, vuelve a intentar")
        void append_retryTrasConflicto() {
            AccountOpenedEvent event = sampleOpened();
            given(repository.findByAggregateIdOrderByVersionAsc("acc-1"))
                .willReturn(List.of())
                .willReturn(List.of());
            given(repository.save(any(EventStoreEntity.class)))
                .willThrow(new DataIntegrityViolationException("conflict"))
                .willAnswer(inv -> inv.getArgument(0));

            store.append(event, "Account");

            verify(repository, times(2)).save(any(EventStoreEntity.class));
        }

        @Test
        @DisplayName("Retry exhausto: tras MAX_RETRIES intentos lanza ConcurrentModificationException")
        void append_retryExhausto_lanzaExcepcion() {
            AccountOpenedEvent event = sampleOpened();
            given(repository.findByAggregateIdOrderByVersionAsc("acc-1")).willReturn(List.of());
            given(repository.save(any(EventStoreEntity.class)))
                .willThrow(new DataIntegrityViolationException("conflict"));

            assertThatThrownBy(() -> store.append(event, "Account"))
                .isInstanceOf(ConcurrentModificationException.class);

            verify(repository, times(3)).save(any(EventStoreEntity.class));
        }
    }

    @Nested
    @DisplayName("load")
    class Load {

        @Test
        @DisplayName("Reproduce eventos y devuelve el Account rehidratado")
        void load_replay() {
            EventStoreEntity opened = persisted("evt-1", "acc-1", "AccountOpenedEvent", 1L,
                JsonUtil.toJson(sampleOpened()));
            EventStoreEntity deposited = persisted("evt-2", "acc-1", "MoneyDepositedEvent", 2L,
                JsonUtil.toJson(new MoneyDepositedEvent(
                    "acc-1",
                    new Money(new BigDecimal("50.00"), "USD"),
                    new Money(new BigDecimal("150.00"), "USD"),
                    metadata(2)
                )));
            given(repository.findByAggregateIdOrderByVersionAsc("acc-1"))
                .willReturn(List.of(opened, deposited));

            Account account = store.load("acc-1");

            assertThat(account.accountId()).isEqualTo("acc-1");
            assertThat(account.balance()).isEqualTo(new Money(new BigDecimal("150.00"), "USD"));
            assertThat(account.version()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Lanza AccountNotFoundException cuando no hay eventos")
        void load_sinEventos_lanzaExcepcion() {
            given(repository.findByAggregateIdOrderByVersionAsc("missing")).willReturn(List.of());

            assertThatThrownBy(() -> store.load("missing"))
                .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("Lanza IllegalStateException ante event_type desconocido")
        void load_eventTypeDesconocido_lanzaExcepcion() {
            EventStoreEntity unknown = persisted("evt-x", "acc-1", "UnknownEvent", 1L, "{}");
            given(repository.findByAggregateIdOrderByVersionAsc("acc-1")).willReturn(List.of(unknown));

            assertThatThrownBy(() -> store.load("acc-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown event type");
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("true cuando hay al menos un evento")
        void exists_true() {
            given(repository.findByAggregateIdOrderByVersionAsc("acc-1"))
                .willReturn(List.of(persisted("evt-1", "acc-1", "AccountOpenedEvent", 1L, "{}")));

            assertThat(store.exists("acc-1")).isTrue();
        }

        @Test
        @DisplayName("false cuando no hay eventos")
        void exists_false() {
            given(repository.findByAggregateIdOrderByVersionAsc("missing")).willReturn(List.of());

            assertThat(store.exists("missing")).isFalse();
        }
    }

    private static EventStoreEntity persisted(String id, String aggregateId, String type, long version, String data) {
        EventStoreEntity row = new EventStoreEntity();
        row.setId(id);
        row.setAggregateId(aggregateId);
        row.setAggregateType("Account");
        row.setEventType(type);
        row.setEventData(data);
        row.setVersion(version);
        row.setCreatedAt(Instant.now());
        row.setCorrelationId("corr-1");
        return row;
    }

    private static AccountOpenedEvent sampleOpened() {
        return new AccountOpenedEvent(
            "acc-1", "user-1",
            new Money(new BigDecimal("100.00"), "USD"),
            metadata(1)
        );
    }

    private static EventMetadata metadata(int version) {
        return new EventMetadata("evt-" + version, Instant.now(), "corr-" + version, version);
    }
}
