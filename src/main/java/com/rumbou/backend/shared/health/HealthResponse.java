package com.rumbou.backend.shared.health;

import java.time.Instant;

public record HealthResponse(
        String status,
        String servicio,
        Instant timestamp
) {
}
