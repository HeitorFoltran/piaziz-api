package com.azizaid.hub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Fora da classe @SpringBootApplication para que as rotinas possam ser desligadas nos testes
// (cron "-" em src/test/resources/application.properties).
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
