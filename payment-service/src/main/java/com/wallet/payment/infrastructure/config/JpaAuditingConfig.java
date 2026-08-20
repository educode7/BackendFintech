package com.wallet.payment.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing so {@code @CreatedDate} / {@code @LastModifiedDate}
 * fields on the {@code Payment} entity are populated automatically by Hibernate.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
