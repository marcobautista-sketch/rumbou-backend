package com.rumbou.backend.dto.response;

import java.time.LocalDateTime;

public record LogroResponse(
        Long id,
        String nombre,
        String descripcion,
        LocalDateTime fechaDesbloqueo
) {
}
