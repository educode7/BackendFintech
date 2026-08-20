package com.wallet.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config Server.
 *
 * <h2>Why a dedicated config service?</h2>
 * Centralizing configuration into a single service gives us:
 * <ul>
 *   <li><b>Single source of truth</b>: all service configs live in one Git repo (config-repo/).</li>
 *   <li><b>Runtime refresh</b>: change a config → POST /actuator/refresh → no restart needed.</li>
 *   <li><b>Version history</b>: every config change is a Git commit with author, message, and diff.</li>
 *   <li><b>Environment promotion</b>: dev → staging → prod by branching the config repo.</li>
 *   <li><b>Encryption</b>: encrypt secrets at rest in Git, decrypt at serve time.</li>
 * </ul>
 *
 * <h2>Config hierarchy</h2>
 * <pre>
 *   application.yml          ← shared across ALL services (virtual threads, HikariCP, Kafka, actuator)
 *   account-service.yml      ← service-specific overrides (datasource, port, resilience4j)
 *   payment-service.yml      ← service-specific overrides
 *   notification-service.yml ← service-specific overrides
 *   application-docker.yml   ← profile overlay for Docker (host → container names)
 * </pre>
 *
 * <h2>Client resolution</h2>
 * Each service's bootstrap.yml points to this server. At startup, the client fetches:
 *   1. application.yml (shared)
 *   2. {service-name}.yml (service-specific)
 *   3. application-{profile}.yml (profile overlay, e.g. docker)
 *
 * Properties are merged in order; later sources override earlier ones.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }
}
