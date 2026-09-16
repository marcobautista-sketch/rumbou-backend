package com.rumbou.backend.shared.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

// Unico endpoint publico que no es de auth ni de webhooks. Sirve para dos cosas:
// que la URL de produccion muestre algo vivo al abrirla, y como health check
// de la plataforma de despliegue (Railway, y ECS cuando se migre a AWS): si
// responde 200, el contenedor esta sano. No toca la base de datos a proposito,
// para que una caida de Postgres no tumbe el contenedor entero.
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
