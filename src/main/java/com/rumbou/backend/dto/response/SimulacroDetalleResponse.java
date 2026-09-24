package com.rumbou.backend.dto.response;

import com.rumbou.backend.entity.EstadoSimulacro;
import com.rumbou.backend.entity.TipoSimulacro;

import java.time.LocalDateTime;
import java.util.List;

public record SimulacroDetalleResponse(
        Long id,
        TipoSimulacro tipo,
        EstadoSimulacro estado,
        Long areaId,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        Double puntajeObtenido,
        Double psp,
        List<RespuestaDetalleResponse> preguntas
) {
}
