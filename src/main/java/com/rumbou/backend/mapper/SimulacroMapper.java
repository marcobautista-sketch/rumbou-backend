package com.rumbou.backend.mapper;

import com.rumbou.backend.dto.response.PreguntaSimulacroResponse;
import com.rumbou.backend.dto.response.RespuestaDetalleResponse;
import com.rumbou.backend.dto.response.ResultadoSimulacroResponse;
import com.rumbou.backend.dto.response.SimulacroDetalleResponse;
import com.rumbou.backend.dto.response.SimulacroResponse;
import com.rumbou.backend.dto.response.SimulacroResumenResponse;
import com.rumbou.backend.entity.EstadoSimulacro;
import com.rumbou.backend.entity.Pregunta;
import com.rumbou.backend.entity.RespuestaUsuario;
import com.rumbou.backend.entity.Simulacro;

import java.util.List;

public final class SimulacroMapper {

    private SimulacroMapper() {
    }

    public static SimulacroResponse toResponse(Simulacro simulacro, List<RespuestaUsuario> respuestas) {
        List<PreguntaSimulacroResponse> preguntas = respuestas.stream()
                .map(r -> new PreguntaSimulacroResponse(
                        r.getId(),
                        r.getPregunta().getId(),
                        r.getPregunta().getEnunciado(),
                        r.getPregunta().getAlternativas()))
                .toList();

        return new SimulacroResponse(
                simulacro.getId(), simulacro.getTipo(), simulacro.getEstado(),
                simulacro.getFechaInicio(), preguntas);
    }

    public static ResultadoSimulacroResponse toResultado(Simulacro simulacro) {
        return new ResultadoSimulacroResponse(
                simulacro.getId(), simulacro.getPuntajeObtenido(), simulacro.getPsp(), simulacro.getEstado());
    }

    public static SimulacroResumenResponse toResumen(Simulacro simulacro) {
        return new SimulacroResumenResponse(
                simulacro.getId(),
                simulacro.getTipo(),
                simulacro.getEstado(),
                simulacro.getArea().getId(),
                simulacro.getArea().getCodigo(),
                simulacro.getFechaInicio(),
                simulacro.getFechaFin(),
                simulacro.getPuntajeObtenido(),
                simulacro.getPsp());
    }

    public static SimulacroDetalleResponse toDetalle(Simulacro simulacro, List<RespuestaUsuario> respuestas) {
        boolean finalizado = simulacro.getEstado() == EstadoSimulacro.FINALIZADO;
        List<RespuestaDetalleResponse> preguntas = respuestas.stream()
                .map(r -> toRespuestaDetalle(r, finalizado))
                .toList();

        return new SimulacroDetalleResponse(
                simulacro.getId(),
                simulacro.getTipo(),
                simulacro.getEstado(),
                simulacro.getArea().getId(),
                simulacro.getFechaInicio(),
                simulacro.getFechaFin(),
                simulacro.getPuntajeObtenido(),
                simulacro.getPsp(),
                preguntas);
    }

    // Mientras el simulacro esta en curso no se revela la clave ni la explicacion.
    private static RespuestaDetalleResponse toRespuestaDetalle(RespuestaUsuario respuesta, boolean finalizado) {
        Pregunta pregunta = respuesta.getPregunta();
        return new RespuestaDetalleResponse(
                respuesta.getId(),
                pregunta.getId(),
                pregunta.getEnunciado(),
                pregunta.getAlternativas(),
                respuesta.getAlternativaMarcada(),
                finalizado ? pregunta.getClaveCorrecta() : null,
                finalizado ? respuesta.getEsCorrecta() : null,
                finalizado ? respuesta.getPuntajeAportado() : null,
                finalizado ? pregunta.getExplicacion() : null);
    }
}
