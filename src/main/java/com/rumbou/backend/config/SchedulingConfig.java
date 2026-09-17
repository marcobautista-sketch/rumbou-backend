package com.rumbou.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Activa las tareas programadas (@Scheduled) de todo el proyecto. Vive en
// config/ por decision del equipo; el job de suscripciones vencidas es del
// scheduler/.
@Configuration
@EnableScheduling
public class SchedulingConfig {
}