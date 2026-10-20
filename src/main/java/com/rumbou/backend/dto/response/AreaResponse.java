package com.rumbou.backend.dto.response;

import java.time.LocalDate;

public record AreaResponse(
        Long id,
        String universidad,
        String codigo,
        String nombre,
        String procesoProximo,
        LocalDate fechaProximoExamen,
        Boolean fechaExamenConfirmada
) {
}
