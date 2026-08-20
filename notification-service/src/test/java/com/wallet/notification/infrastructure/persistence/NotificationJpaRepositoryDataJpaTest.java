package com.wallet.notification.infrastructure.persistence;

import com.wallet.notification.domain.Notification;
import com.wallet.notification.domain.NotificationStatus;
import com.wallet.notification.domain.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:notificationdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false"
})
class NotificationJpaRepositoryDataJpaTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private NotificationJpaRepository repo;

    @Test
    @DisplayName("save y findById recuperan la notificación persistida")
    void save_yFindById() {
        Notification saved = persist(sample("notif-1", "user-1", "evt-1"));

        Optional<Notification> found = repo.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo("user-1");
        assertThat(found.get().getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    @DisplayName("findByUserIdOrderByCreatedAtDesc pagina resultados")
    void findByUserId_paginado() {
        persist(sample("notif-1", "user-1", "evt-1"));
        persist(sample("notif-2", "user-1", "evt-2"));
        persist(sample("notif-3", "user-2", "evt-3"));

        Page<Notification> page = repo.findByUserIdOrderByCreatedAtDesc("user-1", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("deleteById elimina la notificación")
    void deleteById_elimina() {
        Notification saved = persist(sample("notif-del", "user-1", "evt-1"));

        repo.deleteById(saved.getId());
        em.flush();

        assertThat(repo.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("La constraint UNIQUE sobre processedEventId rechaza duplicados")
    void processedEventIdUnico() {
        persist(sample("notif-1", "user-1", "evt-unique"));

        org.assertj.core.api.Assertions
            .assertThatThrownBy(() -> persist(sample("notif-2", "user-2", "evt-unique")))
            .isInstanceOfAny(
                org.springframework.dao.DataIntegrityViolationException.class,
                org.hibernate.exception.ConstraintViolationException.class
            );
    }

    private static Notification sample(String id, String userId, String eventId) {
        Notification n = new Notification();
        n.setId(id);
        n.setUserId(userId);
        n.setType(NotificationType.EMAIL);
        n.setSubject("Test");
        n.setBody("Body");
        n.setStatus(NotificationStatus.PENDING);
        n.setProcessedEventId(eventId);
        n.setCreatedAt(Instant.now());
        return n;
    }

    private Notification persist(Notification n) {
        Notification saved = em.persistAndFlush(n);
        em.clear();
        return saved;
    }
}
