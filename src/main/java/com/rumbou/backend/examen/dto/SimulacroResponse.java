package com.rumbou.backend.examen.dto;

import com.rumbou.backend.examen.EstadoSimulacro;
import com.rumbou.backend.examen.TipoSimulacro;

import java.time.LocalDateTime;
import java.util.List;

public record SimulacroResponse(
        Long id,
        TipoSimulacro tipo,
        EstadoSimulacro estado,
        LocalDateTime fechaInicio,
        List<PreguntaSimulacroResponse> preguntas
) {
}
