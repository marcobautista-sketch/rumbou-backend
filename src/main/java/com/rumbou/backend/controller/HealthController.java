package com.rumbou.backend.controller;

import com.rumbou.backend.dto.response.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

// Unico endpoint publico fuera de auth y webhooks: health check de la plataforma
// de despliegue. No toca la base de datos a proposito.
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final String nombreServicio;

    public HealthController(@Value("${spring.application.name}") String nombreServicio) {
        this.nombreServicio = nombreServicio;
    }

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("ok", nombreServicio, Instant.now());
    }
}
