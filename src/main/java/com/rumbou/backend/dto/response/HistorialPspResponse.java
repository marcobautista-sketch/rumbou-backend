package com.rumbou.backend.dto.response;

import com.rumbou.backend.entity.TipoSimulacro;

import java.time.LocalDateTime;

public record HistorialPspResponse(
        Long simulacroId,
        TipoSimulacro tipo,
        LocalDateTime fechaFin,
        Double puntajeObtenido,
        Double psp
) {
}
