package com.rumbou.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Activa @Scheduled para todo el proyecto; los jobs viven en scheduler/.
@Configuration
@EnableScheduling
public class SchedulingConfig {
}