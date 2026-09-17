package com.rumbou.backend.dto.response;

import java.time.LocalDate;

public record SuscripcionResponse(
        Long id,
        String plan,
        String estado,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String linkPago
) {
}