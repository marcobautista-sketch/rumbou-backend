package com.rumbou.backend.dto.response;

import com.rumbou.backend.entity.EstadoSimulacro;

public record ResultadoSimulacroResponse(
        Long simulacroId,
        double puntajeObtenido,
        double psp,
        EstadoSimulacro estado
) {
}
