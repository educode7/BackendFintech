package com.wallet.payment.infrastructure.config;

import com.wallet.shared.filter.CorrelationIdFilter;
import com.wallet.shared.filter.RequestLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers the shared servlet filters in the right order.
 *
 * Order matters:
 *  1. CorrelationIdFilter — must run first so every subsequent log line carries the ID.
 *  2. RequestLoggingFilter — wraps the whole request so the duration covers all filters.
 */
@Configuration
public class ServletFiltersConfig {

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> reg = new FilterRegistrationBean<>(new CorrelationIdFilter());
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.addUrlPatterns("/*");
        return reg;
    }

    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter() {
        FilterRegistrationBean<RequestLoggingFilter> reg = new FilterRegistrationBean<>(new RequestLoggingFilter());
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        reg.addUrlPatterns("/*");
        return reg;
    }
}
