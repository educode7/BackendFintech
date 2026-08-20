package com.wallet.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Sanity check: the Spring context loads.
 *
 * Uses the {@code test} profile (H2) and disables Kafka + Redis auto-config
 * via application-test.yml so no Docker infrastructure is needed.
 * Infrastructure beans are mocked so application services can wire.
 * Real integration tests belong in a separate suite (see README §11 — TODOs).
 */
@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceApplicationTests {

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    @SuppressWarnings("rawtypes")
    private KafkaTemplate kafkaTemplate;

    @Test
    void contextLoads() { }
}
