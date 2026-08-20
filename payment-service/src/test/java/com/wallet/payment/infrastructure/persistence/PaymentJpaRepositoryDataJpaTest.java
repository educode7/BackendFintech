package com.wallet.payment.infrastructure.persistence;

import com.wallet.payment.domain.Payment;
import com.wallet.payment.domain.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Slice JPA test para {@link PaymentJpaRepository}.
 *
 * <p>Spring Boot 4 moved {@code @DataJpaTest} and {@code TestEntityManager} to
 * the {@code org.springframework.boot.data.jpa.test.autoconfigure} and
 * {@code org.springframework.boot.jpa.test.autoconfigure} packages respectively.
 * The old {@code @AutoConfigureTestDatabase} annotation is no longer needed —
 * {@code @DataJpaTest} no longer requires a real DataSource by default in 4.x.
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:paymentdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false"
})
class PaymentJpaRepositoryDataJpaTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PaymentJpaRepository repo;

    @Test
    @DisplayName("save y findById recuperan el payment persistido")
    void save_yFindById_recupera() {
        Payment saved = persist(payment("idem-1", "user-1"));

        Optional<Payment> found = repo.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo("user-1");
        assertThat(found.get().getAmount()).isEqualByComparingTo("100.00");
        assertThat(found.get().getCurrency()).isEqualTo("USD");
        assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(found.get().getIdempotencyKey()).isEqualTo("idem-1");
    }

    @Test
    @DisplayName("findByIdempotencyKey retorna el pago cuando existe")
    void findByIdempotencyKey_existente() {
        persist(payment("idem-key-1", "user-1"));

        Optional<Payment> found = repo.findByIdempotencyKey("idem-key-1");

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo("user-1");
    }

    @Test
    @DisplayName("findByIdempotencyKey retorna Optional.empty cuando no existe")
    void findByIdempotencyKey_inexistente() {
        Optional<Payment> found = repo.findByIdempotencyKey("does-not-exist");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("El repositorio expone findAll paginado (Page<Payment>)")
    void findAll_paginado() {
        persist(payment("idem-1", "user-1"));
        persist(payment("idem-2", "user-1"));
        persist(payment("idem-3", "user-2"));

        Page<Payment> page = repo.findAll(PageRequest.of(0, 2));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("save asigna version=0 en la primera inserción")
    void save_asignaVersionInicial() {
        Payment saved = persist(payment("idem-v", "user-1"));

        assertThat(saved.getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("deleteById elimina el pago del repositorio")
    void deleteById_elimina() {
        Payment saved = persist(payment("idem-del", "user-1"));

        repo.deleteById(saved.getId());
        em.flush();

        assertThat(repo.findById(saved.getId())).isEmpty();
    }

    private Payment payment(String idemKey, String userId) {
        Payment p = new Payment();
        p.setId("pay-" + idemKey);
        p.setUserId(userId);
        p.setAmount(new BigDecimal("100.00"));
        p.setCurrency("USD");
        p.setStatus(PaymentStatus.COMPLETED);
        p.setIdempotencyKey(idemKey);
        // @DataJpaTest no activa @EnableJpaAuditing — rellenamos manualmente
        // lo que el listener JPA rellenaría en producción.
        java.time.Instant now = java.time.Instant.now();
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        return p;
    }

    private Payment persist(Payment p) {
        Payment saved = em.persistAndFlush(p);
        em.clear();
        return saved;
    }
}
