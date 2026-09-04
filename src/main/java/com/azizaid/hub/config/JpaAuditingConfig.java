package com.azizaid.hub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "profissionalAuditorAware")
public class JpaAuditingConfig {
}
