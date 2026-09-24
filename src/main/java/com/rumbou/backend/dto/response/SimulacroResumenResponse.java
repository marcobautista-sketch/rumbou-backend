package com.rumbou.backend.dto.response;

import com.rumbou.backend.entity.EstadoSimulacro;
import com.rumbou.backend.entity.TipoSimulacro;

import java.time.LocalDateTime;

// Una fila del historial de simulacros del usuario, sin las preguntas.
public record SimulacroResumenResponse(
        Long id,
        TipoSimulacro tipo,
        EstadoSimulacro estado,
        Long areaId,
        String area,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        Double puntajeObtenido,
        Double psp
) {
}
