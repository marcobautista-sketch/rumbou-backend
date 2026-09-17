package com.rumbou.backend.dto.response;

import java.time.Instant;

public record HealthResponse(
        String status,
        String servicio,
        Instant timestamp
) {
}
