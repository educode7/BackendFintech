package com.wallet.account;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Sanity check: el Spring context carga.
 *
 * Uses the {@code test} profile so it doesn't require live Postgres / Redis / Kafka.
 * Real integration tests live in {@code *IT.java} classes.
 */
@SpringBootTest
@ActiveProfiles("test")
class AccountServiceApplicationTests {

    @SuppressWarnings("rawtypes")
    @MockitoBean
    private KafkaTemplate kafkaTemplate;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void contextLoads() { }
}
