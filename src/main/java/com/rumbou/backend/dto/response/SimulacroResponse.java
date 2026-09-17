package com.rumbou.backend.dto.response;

import com.rumbou.backend.entity.EstadoSimulacro;
import com.rumbou.backend.entity.TipoSimulacro;
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
