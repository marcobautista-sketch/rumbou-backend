package com.rumbou.backend.examen.dto;

import com.rumbou.backend.examen.EstadoSimulacro;

public record ResultadoSimulacroResponse(
        Long simulacroId,
        double puntajeObtenido,
        double psp,
        EstadoSimulacro estado
) {
}
